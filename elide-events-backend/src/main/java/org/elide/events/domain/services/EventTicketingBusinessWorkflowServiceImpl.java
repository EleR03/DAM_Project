package org.elide.events.domain.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.event.EventSalesView;
import org.elide.events.domain.event.EventSeat;
import org.elide.events.domain.event.EventStatus;
import org.elide.events.domain.event.SeatStatus;
import org.elide.events.domain.identity.AccountStatus;
import org.elide.events.domain.identity.UserAccount;
import org.elide.events.domain.identity.UserRole;
import org.elide.events.domain.sales.HoldStatus;
import org.elide.events.domain.sales.Order;
import org.elide.events.domain.sales.OrderItem;
import org.elide.events.domain.sales.OrderStatus;
import org.elide.events.domain.sales.SeatHold;
import org.elide.events.domain.sales.SeatHoldItem;
import org.elide.events.domain.sales.Ticket;
import org.elide.events.domain.sales.TicketScanResult;
import org.elide.events.domain.sales.TicketStatus;
import org.elide.events.domain.sales.TicketValidationView;
import org.elide.events.domain.venue.Hall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Simple-Facade strategy: orchestrates factory, repositories, validation and computation services.
 * Seat state changes are serialized with one lock (in-memory stand-in for the DB transaction +
 * row locking that PostgreSQL will provide in the persistence sprint).
 */
@Service("EventTicketingBusinessWorkflowServiceFacade")
public class EventTicketingBusinessWorkflowServiceImpl implements IEventTicketingBusinessWorkflowService {

	public static final int SEAT_HOLD_MINUTES = 10;
	/** Maximum tickets (held + bought) one user can have for ONE event. */
	public static final int MAX_TICKETS_PER_USER = 8;
	/** A started payment (card being charged) keeps its seats for this long after the hold's expiry, waiting for the result. */
	public static final int PAYMENT_GRACE_MINUTES = 5;

	private final Object seatLock = new Object();

	// Support Services
	@Autowired private IUserAccountRepository userRepository;
	@Autowired private IHallRepository hallRepository;
	@Autowired private IEventRepository eventRepository;
	@Autowired private ISeatHoldRepository seatHoldRepository;
	@Autowired private IOrderRepository orderRepository;
	@Autowired private ITicketRepository ticketRepository;

	@Autowired @Qualifier("EventEntityFactoryBase")
	private IEventEntityFactory entityFactory;

	// Domain Services
	@Autowired private IValidatingEventDomainService validatingService;
	@Autowired private ISummarizingEventDomainService summarizingService;
	@Autowired private IPricingOrderDomainService pricingService;
	@Autowired private IConsolidatingEventSalesViewDomainService consolidatingService;

	@Autowired private Clock clock;

	private LocalDateTime now() {
		return LocalDateTime.now(clock);
	}

	// (0) administrator approves an organizer account: PENDING -> ACTIVE
	@Override
	public Integer approveOrganizer(Integer adminId, Integer organizerId) {
		synchronized (seatLock) {
			UserAccount admin = userRepository.getById(adminId);
			if (admin == null || admin.getRole() != UserRole.ADMINISTRATOR || !admin.isActive())
				throw new BusinessRuleViolationException("Only an ACTIVE administrator can approve organizers!");
			UserAccount organizer = userRepository.getById(organizerId);
			if (organizer == null || organizer.getRole() != UserRole.ORGANIZER)
				throw new BusinessRuleViolationException("Organizer not found: " + organizerId);
			if (organizer.getStatus() != AccountStatus.PENDING)
				throw new BusinessRuleViolationException("Only a PENDING organizer can be approved, current: " + organizer.getStatus());
			organizer.setStatus(AccountStatus.ACTIVE);
			return organizer.getUserId();
		}
	}

	// (1)
	@Override
	public Integer createDraftEvent(Integer organizerId, Integer hallId, String title,
			LocalDateTime startAt, LocalDateTime endAt, BigDecimal basePrice) {
		UserAccount organizer = userRepository.getById(organizerId);
		if (organizer == null || organizer.getRole() != UserRole.ORGANIZER
				|| (!organizer.isActive() && organizer.getStatus() != AccountStatus.PENDING))
			throw new BusinessRuleViolationException("Only an organizer that is ACTIVE or waiting for approval (PENDING) can create draft events!");
		Hall hall = hallRepository.getById(hallId);
		if (hall == null)
			throw new BusinessRuleViolationException("Hall not found: " + hallId);

		Event event = entityFactory.buildEventWithSeats(organizerId, hall, title, startAt, endAt, basePrice);
		validatingService.validateWithException(event);
		eventRepository.add(event);
		return event.getEventId();
	}

	// (2)
	@Override
	public Integer publishEvent(Integer eventId) {
		synchronized (seatLock) {
			Event event = requireEvent(eventId);
			if (event.getStatus() != EventStatus.DRAFT)
				throw new BusinessRuleViolationException("Only a DRAFT event can be published, current: " + event.getStatus());
			UserAccount organizer = userRepository.getById(event.getOrganizerId());
			if (organizer == null || !organizer.isActive())
				throw new BusinessRuleViolationException("Event cannot be published: the organizer account is not approved (status "
						+ (organizer == null ? "UNKNOWN" : organizer.getStatus()) + ")!");
			validatingService.validateWithException(event);
			validatingService.validateEventAggregate(event);
			event.setStatus(EventStatus.PUBLISHED);
			event.setPublishedAt(now());
			return event.getEventId();
		}
	}

	@Override
	public Integer rescheduleEvent(Integer eventId, LocalDateTime newStartAt, LocalDateTime newEndAt) {
		synchronized (seatLock) {
			Event event = requireEvent(eventId);
			if (event.getStatus() != EventStatus.DRAFT && event.getStatus() != EventStatus.PUBLISHED)
				throw new BusinessRuleViolationException("Event cannot be changed in status " + event.getStatus());
			if (event.getStatus() == EventStatus.PUBLISHED && hasSalesOrHolds(event))
				throw new BusinessRuleViolationException("Published event with sold or held seats cannot change its date!");

			LocalDateTime oldStart = event.getStartAt();
			LocalDateTime oldEnd = event.getEndAt();
			event.setStartAt(newStartAt);
			event.setEndAt(newEndAt);
			try {
				validatingService.validateWithException(event);
			} catch (DomainValidationException ex) {
				event.setStartAt(oldStart);
				event.setEndAt(oldEnd);
				throw ex;
			}
			return event.getEventId();
		}
	}

	// (2) reduce the capacity of a ticket category (never below sold + held tickets)
	@Override
	public Integer reduceCategoryCapacity(Integer eventId, String ticketTypeName, int newCapacity) {
		synchronized (seatLock) {
			Event event = requireEvent(eventId);
			if (event.getStatus() != EventStatus.DRAFT && event.getStatus() != EventStatus.PUBLISHED)
				throw new BusinessRuleViolationException("Capacity cannot be changed in status " + event.getStatus());
			if (event.findTicketType(ticketTypeName) == null)
				throw new BusinessRuleViolationException("Unknown ticket category: " + ticketTypeName);
			if (newCapacity < 0)
				throw new BusinessRuleViolationException("Capacity must not be negative!");

			List<EventSeat> categorySeats = new ArrayList<>();
			for (EventSeat seat : event.getEventSeats())
				if (seat.getTicketType().getName().equalsIgnoreCase(ticketTypeName) && seat.getStatus() != SeatStatus.BLOCKED)
					categorySeats.add(seat);

			int currentCapacity = categorySeats.size();
			int committed = (int) categorySeats.stream()
					.filter(s -> s.getStatus() == SeatStatus.SOLD || s.getStatus() == SeatStatus.HELD).count();
			if (newCapacity > currentCapacity)
				throw new BusinessRuleViolationException("This operation only reduces capacity (current " + currentCapacity + ")!");
			if (newCapacity < committed)
				throw new BusinessRuleViolationException("Capacity of " + ticketTypeName + " cannot be reduced below the "
						+ committed + " tickets already sold or held!");

			// block the surplus AVAILABLE seats, highest seat id first
			int toBlock = currentCapacity - newCapacity;
			for (int i = categorySeats.size() - 1; i >= 0 && toBlock > 0; i--) {
				EventSeat seat = categorySeats.get(i);
				if (seat.isAvailable()) {
					seat.block();
					toBlock--;
				}
			}
			return currentCapacity - newCapacity;
		}
	}

	// (2) cancel: compensation flow, never blocked by sold tickets
	@Override
	public List<Integer> cancelEvent(Integer eventId) {
		synchronized (seatLock) {
			Event event = requireEvent(eventId);
			if (event.getStatus() == EventStatus.CANCELLED)
				return new ArrayList<>(); // idempotent: nothing more to do
			if (event.getStatus() == EventStatus.COMPLETED || event.getStatus() == EventStatus.DELETED)
				throw new BusinessRuleViolationException("Event cannot be cancelled in status " + event.getStatus());
			if (event.getEndAt().isBefore(now()))
				throw new BusinessRuleViolationException("Event already ended, it cannot be cancelled!");

			event.setStatus(EventStatus.CANCELLED);
			cancelActiveHoldsOf(eventId);

			List<Integer> refundOrderIds = new ArrayList<>();
			for (Order order : orderRepository.toCollection()) {
				if (order.getEventId().equals(eventId) && order.getStatus() == OrderStatus.PAID) {
					order.setStatus(OrderStatus.REFUNDED);
					refundOrderIds.add(order.getOrderId());
				}
			}
			for (Ticket ticket : ticketRepository.findByEventId(eventId))
				if (ticket.getStatus() == TicketStatus.ISSUED)
					ticket.setStatus(TicketStatus.REFUNDED);
			return refundOrderIds;
		}
	}

	// (2) delete: only when nothing was sold (financial records must stay consistent)
	@Override
	public Integer deleteEvent(Integer eventId) {
		synchronized (seatLock) {
			Event event = requireEvent(eventId);
			boolean hasSoldSeats = event.getEventSeats().stream().anyMatch(s -> s.getStatus() == SeatStatus.SOLD);
			if (hasSoldSeats)
				throw new BusinessRuleViolationException("Event with sold tickets cannot be deleted, cancel it instead!");
			cancelActiveHoldsOf(eventId);
			eventRepository.remove(event);
			return eventId;
		}
	}

	// (3)
	@Override
	public Integer holdSeats(Integer userId, Integer eventId, List<Integer> eventSeatIds) {
		synchronized (seatLock) {
			LocalDateTime now = now();
			releaseExpiredHolds(now);

			UserAccount user = userRepository.getById(userId);
			if (user == null || !user.isActive())
				throw new BusinessRuleViolationException("Unknown or inactive user: " + userId);
			Event event = requireEvent(eventId);
			if (event.getStatus() != EventStatus.PUBLISHED)
				throw new BusinessRuleViolationException("Seats can be held only for PUBLISHED events, current: " + event.getStatus());
			if (eventSeatIds == null || eventSeatIds.isEmpty())
				throw new BusinessRuleViolationException("At least one seat must be selected!");

			// all-or-nothing: check every seat before changing any
			Set<Integer> distinctIds = new LinkedHashSet<>(eventSeatIds);
			if (seatsOwnedByUser(userId, eventId) + distinctIds.size() > MAX_TICKETS_PER_USER)
				throw new TicketLimitExceededException(MAX_TICKETS_PER_USER);
			List<EventSeat> seats = new ArrayList<>();
			for (Integer seatId : distinctIds) {
				EventSeat seat = event.findEventSeat(seatId);
				if (seat == null)
					throw new BusinessRuleViolationException("Unknown event seat: " + seatId);
				if (!seat.isAvailable())
					throw new SeatNoLongerAvailableException(seat.getLabel());
				seats.add(seat);
			}

			SeatHold hold = entityFactory.buildSeatHold(userId, event, distinctIds, now, SEAT_HOLD_MINUTES);
			for (EventSeat seat : seats)
				seat.hold(hold.getHoldId(), hold.getExpiresAt());
			seatHoldRepository.add(hold);
			return hold.getHoldId();
		}
	}

	// (4)
	@Override
	public Integer createOrder(Integer holdId) {
		synchronized (seatLock) {
			LocalDateTime now = now();
			SeatHold hold = requireHold(holdId);
			if (hold.getStatus() == HoldStatus.ACTIVE && hold.isExpiredAt(now))
				expireHold(hold);
			if (hold.getStatus() != HoldStatus.ACTIVE)
				throw new BusinessRuleViolationException("Seat hold is not active (status " + hold.getStatus() + ")!");
			boolean orderExists = orderRepository.toCollection().stream()
					.anyMatch(o -> holdId.equals(o.getHoldId()) && isUnpaid(o));
			if (orderExists)
				throw new BusinessRuleViolationException("An order already exists for hold " + holdId);

			Event event = requireEvent(hold.getEventId());
			Order order = entityFactory.buildOrder(hold, event);
			pricingService.computingTotal(order);
			validatingService.validateWithException(order);
			orderRepository.add(order);
			return order.getOrderId();
		}
	}

	// (5) the payment layer asks permission BEFORE charging the card
	@Override
	public Integer startPayment(Integer orderId) {
		synchronized (seatLock) {
			LocalDateTime now = now();
			Order order = requireOrder(orderId);
			if (order.getStatus() == OrderStatus.EXPIRED)
				throw new PaymentRejectedException();
			if (order.getStatus() != OrderStatus.PENDING)
				throw new BusinessRuleViolationException("Payment can start only for a PENDING order (status " + order.getStatus() + ")!");
			SeatHold hold = requireHold(order.getHoldId());
			if (hold.getStatus() == HoldStatus.ACTIVE && hold.isExpiredAt(now))
				expireHold(hold); // seats released, order EXPIRED
			if (hold.getStatus() != HoldStatus.ACTIVE)
				throw new PaymentRejectedException();
			order.setStatus(OrderStatus.PAYMENT_PROCESSING); // seats stay held until the payment result arrives
			return order.getOrderId();
		}
	}

	// (5) payment success (webhook)
	@Override
	public List<Integer> confirmPayment(Integer orderId) {
		synchronized (seatLock) {
			LocalDateTime now = now();
			Order order = requireOrder(orderId);
			boolean paymentStarted = order.getStatus() == OrderStatus.PAYMENT_PROCESSING;
			if (order.getStatus() != OrderStatus.PENDING && !paymentStarted)
				throw new BusinessRuleViolationException("Order is not waiting for payment (status " + order.getStatus() + ")!");
			SeatHold hold = requireHold(order.getHoldId());
			// a payment started in time is honoured even if the result arrives a little after the 10 minutes
			if (hold.getStatus() != HoldStatus.ACTIVE || (!paymentStarted && hold.isExpiredAt(now))) {
				if (hold.getStatus() == HoldStatus.ACTIVE)
					expireHold(hold);
				order.setStatus(OrderStatus.EXPIRED);
				throw new BusinessRuleViolationException("Seat hold expired before payment; the payment must be refunded!");
			}

			Event event = requireEvent(order.getEventId());
			for (SeatHoldItem item : hold.getItems())
				event.findEventSeat(item.getEventSeatId()).sell();
			hold.setStatus(HoldStatus.CONVERTED);
			order.setStatus(OrderStatus.PAID);
			order.setPaidAt(now);

			List<Integer> ticketIds = new ArrayList<>();
			for (OrderItem item : order.getItems()) {
				Ticket ticket = entityFactory.buildTicket(order, item, now);
				ticketRepository.add(ticket);
				ticketIds.add(ticket.getTicketId());
			}
			return ticketIds;
		}
	}

	// (5) payment failure (webhook)
	@Override
	public Integer failPayment(Integer orderId) {
		synchronized (seatLock) {
			Order order = requireOrder(orderId);
			if (!isUnpaid(order))
				throw new BusinessRuleViolationException("Order is not waiting for payment (status " + order.getStatus() + ")!");
			SeatHold hold = requireHold(order.getHoldId());
			if (hold.getStatus() == HoldStatus.ACTIVE) {
				releaseHoldSeats(hold);
				hold.setStatus(HoldStatus.CANCELLED);
			}
			order.setStatus(OrderStatus.FAILED);
			return order.getOrderId();
		}
	}

	// (6) periodic job
	@Override
	public int expireHolds(LocalDateTime now) {
		synchronized (seatLock) {
			return releaseExpiredHolds(now);
		}
	}

	// (7)
	@Override
	public TicketValidationView validateTicket(String qrToken, Integer eventId) {
		synchronized (seatLock) {
			Ticket ticket = ticketRepository.findByQrToken(qrToken);
			if (ticket == null)
				return new TicketValidationView(TicketScanResult.INVALID, null, null, null);
			TicketScanResult result;
			if (!ticket.getEventId().equals(eventId)) {
				result = TicketScanResult.WRONG_EVENT;
			} else if (ticket.getStatus() == TicketStatus.USED) {
				result = TicketScanResult.ALREADY_USED;
			} else if (ticket.getStatus() != TicketStatus.ISSUED) {
				result = TicketScanResult.CANCELLED;
			} else {
				ticket.markUsed(now());
				result = TicketScanResult.VALID;
			}
			return new TicketValidationView(result, ticket.getTicketId(), ticket.getSeatLabel(), ticket.getTicketTypeName());
		}
	}

	// (8)
	@Override
	public EventSalesView getEventSalesReport(Integer eventId) {
		synchronized (seatLock) {
			Event event = requireEvent(eventId);
			return consolidatingService.getEventSalesViewDataOf(event, ticketRepository.findByEventId(eventId));
		}
	}

	/* ------------------------- private helpers ------------------------- */

	private int releaseExpiredHolds(LocalDateTime now) {
		int count = 0;
		for (SeatHold hold : seatHoldRepository.toCollection()) {
			if (hold.getStatus() == HoldStatus.ACTIVE && hold.isExpiredAt(now) && !isPaymentInProgress(hold, now)) {
				expireHold(hold);
				count++;
			}
		}
		return count;
	}

	private void expireHold(SeatHold hold) {
		releaseHoldSeats(hold);
		hold.setStatus(HoldStatus.EXPIRED);
		for (Order order : orderRepository.toCollection())
			if (hold.getHoldId().equals(order.getHoldId()) && isUnpaid(order))
				order.setStatus(OrderStatus.EXPIRED);
	}

	/** An order that has not been paid yet: waiting for the payment, or the card is being charged. */
	private boolean isUnpaid(Order order) {
		return order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.PAYMENT_PROCESSING;
	}

	/** True if the card of this hold is being charged and the payment grace period is not over. */
	private boolean isPaymentInProgress(SeatHold hold, LocalDateTime now) {
		if (now.isAfter(hold.getExpiresAt().plusMinutes(PAYMENT_GRACE_MINUTES)))
			return false;
		for (Order order : orderRepository.toCollection())
			if (hold.getHoldId().equals(order.getHoldId()) && order.getStatus() == OrderStatus.PAYMENT_PROCESSING)
				return true;
		return false;
	}

	private void cancelActiveHoldsOf(Integer eventId) {
		for (SeatHold hold : seatHoldRepository.toCollection()) {
			if (hold.getEventId().equals(eventId) && hold.getStatus() == HoldStatus.ACTIVE) {
				releaseHoldSeats(hold);
				hold.setStatus(HoldStatus.CANCELLED);
				for (Order order : orderRepository.toCollection())
					if (hold.getHoldId().equals(order.getHoldId()) && isUnpaid(order))
						order.setStatus(OrderStatus.CANCELLED);
			}
		}
	}

	private void releaseHoldSeats(SeatHold hold) {
		Event event = eventRepository.getById(hold.getEventId());
		if (event == null)
			return;
		for (SeatHoldItem item : hold.getItems()) {
			EventSeat seat = event.findEventSeat(item.getEventSeatId());
			if (seat != null && seat.getStatus() == SeatStatus.HELD && hold.getHoldId().equals(seat.getHoldId()))
				seat.release();
		}
	}

	/** Seats of the user for this event: currently held (ACTIVE holds) + bought (CONVERTED holds). */
	private int seatsOwnedByUser(Integer userId, Integer eventId) {
		int count = 0;
		for (SeatHold hold : seatHoldRepository.toCollection()) {
			if (hold.getUserId().equals(userId) && hold.getEventId().equals(eventId)
					&& (hold.getStatus() == HoldStatus.ACTIVE || hold.getStatus() == HoldStatus.CONVERTED))
				count += hold.getItems().size();
		}
		return count;
	}

	private boolean hasSalesOrHolds(Event event) {
		return event.getEventSeats().stream()
				.anyMatch(s -> s.getStatus() == SeatStatus.SOLD || s.getStatus() == SeatStatus.HELD);
	}

	private Event requireEvent(Integer eventId) {
		Event event = eventRepository.getById(eventId);
		if (event == null)
			throw new BusinessRuleViolationException("Event not found: " + eventId);
		return event;
	}

	private SeatHold requireHold(Integer holdId) {
		SeatHold hold = seatHoldRepository.getById(holdId);
		if (hold == null)
			throw new BusinessRuleViolationException("Seat hold not found: " + holdId);
		return hold;
	}

	private Order requireOrder(Integer orderId) {
		Order order = orderRepository.getById(orderId);
		if (order == null)
			throw new BusinessRuleViolationException("Order not found: " + orderId);
		return order;
	}
}
