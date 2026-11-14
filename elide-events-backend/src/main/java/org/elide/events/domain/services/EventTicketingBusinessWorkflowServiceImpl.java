package org.elide.events.domain.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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
import org.elide.events.infrastructure.ITransactionManager;
import org.springframework.stereotype.Service;

/**
 * Simple-Facade strategy: orchestrates factory, repositories, validation and computation services.
 *
 * <p>Every workflow step is ONE database transaction (ITransactionManager): all the entities it changes
 * (seats of the event, seat holds, orders and order items, tickets) are committed together or not at all.
 *
 * <p>Concurrency: the Event row is the lock of the aggregate (SELECT ... FOR UPDATE, taken first in every step that
 * changes seat states); a ticket is locked by its own row when it is scanned. Concurrent transactions on the same
 * event/ticket are serialized by PostgreSQL, so no seat is held or sold twice and a ticket admits one entry only.
 * Lock order is always: event row, then rows of its components, so transactions cannot deadlock each other.
 *
 * <p>Steps that must keep a state change although the caller is told "no" (a hold found expired is released and its
 * order expires, then the payment is rejected) commit first and report the failure afterwards.
 */
@Service("EventTicketingBusinessWorkflowServiceFacade")
public class EventTicketingBusinessWorkflowServiceImpl implements IEventTicketingBusinessWorkflowService {

	public static final int SEAT_HOLD_MINUTES = 10;
	/** Maximum tickets (held + bought) one user can have for ONE event. */
	public static final int MAX_TICKETS_PER_USER = 8;
	/** A started payment (card being charged) keeps its seats for this long after the hold's expiry, waiting for the result. */
	public static final int PAYMENT_GRACE_MINUTES = 5;

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

	@Autowired private ITransactionManager transactionManager;
	@Autowired private Clock clock;

	private LocalDateTime now() {
		return LocalDateTime.now(clock);
	}

	// (0) administrator approves an organizer account: PENDING -> ACTIVE
	@Override
	public Integer approveOrganizer(Integer adminId, Integer organizerId) {
		return transactionManager.inTransaction(() -> {
			UserAccount admin = userRepository.getByIdOrNull(adminId);
			if (admin == null || admin.getRole() != UserRole.ADMINISTRATOR || !admin.isActive())
				throw new BusinessRuleViolationException("Only an ACTIVE administrator can approve organizers!");
			UserAccount organizer = userRepository.getByIdForUpdate(organizerId); // two admins cannot approve at once
			if (organizer == null || organizer.getRole() != UserRole.ORGANIZER)
				throw new BusinessRuleViolationException("Organizer not found: " + organizerId);
			if (organizer.getStatus() != AccountStatus.PENDING)
				throw new BusinessRuleViolationException("Only a PENDING organizer can be approved, current: " + organizer.getStatus());
			organizer.setStatus(AccountStatus.ACTIVE);
			return organizer.getUserId();
		});
	}

	// (1)
	@Override
	public Integer createDraftEvent(Integer organizerId, Integer hallId, String title,
			LocalDateTime startAt, LocalDateTime endAt, BigDecimal basePrice) {
		return transactionManager.inTransaction(() -> {
			UserAccount organizer = userRepository.getByIdOrNull(organizerId);
			if (organizer == null || organizer.getRole() != UserRole.ORGANIZER
					|| (!organizer.isActive() && organizer.getStatus() != AccountStatus.PENDING))
				throw new BusinessRuleViolationException("Only an organizer that is ACTIVE or waiting for approval (PENDING) can create draft events!");
			Hall hall = hallRepository.getByIdOrNull(hallId);
			if (hall == null)
				throw new BusinessRuleViolationException("Hall not found: " + hallId);

			Event event = entityFactory.buildEventWithSeats(organizerId, hall, title, startAt, endAt, basePrice);
			validatingService.validateWithException(event);
			eventRepository.add(event); // INSERT event + ticket types + seats (cascade), ids generated here
			return event.getEventId();
		});
	}

	// (2)
	@Override
	public Integer publishEvent(Integer eventId) {
		return transactionManager.inTransaction(() -> {
			Event event = lockEvent(eventId);
			if (event.getStatus() != EventStatus.DRAFT)
				throw new BusinessRuleViolationException("Only a DRAFT event can be published, current: " + event.getStatus());
			UserAccount organizer = userRepository.getByIdOrNull(event.getOrganizerId());
			if (organizer == null || !organizer.isActive())
				throw new BusinessRuleViolationException("Event cannot be published: the organizer account is not approved (status "
						+ (organizer == null ? "UNKNOWN" : organizer.getStatus()) + ")!");
			validatingService.validateWithException(event);
			validatingService.validateEventAggregate(event);
			event.setStatus(EventStatus.PUBLISHED);
			event.setPublishedAt(now());
			return event.getEventId();
		});
	}

	@Override
	public Integer rescheduleEvent(Integer eventId, LocalDateTime newStartAt, LocalDateTime newEndAt) {
		return transactionManager.inTransaction(() -> {
			Event event = lockEvent(eventId);
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
				event.setStartAt(oldStart); // nothing is flushed: the transaction rolls back with the exception
				event.setEndAt(oldEnd);
				throw ex;
			}
			return event.getEventId();
		});
	}

	// (2) reduce the capacity of a ticket category (never below sold + held tickets)
	@Override
	public Integer reduceCategoryCapacity(Integer eventId, String ticketTypeName, int newCapacity) {
		return transactionManager.inTransaction(() -> {
			Event event = lockEvent(eventId);
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
		});
	}

	// (2) cancel: compensation flow, never blocked by sold tickets. ONE transaction: event, holds, seats, orders, tickets.
	@Override
	public List<Integer> cancelEvent(Integer eventId) {
		return transactionManager.inTransaction(() -> {
			Event event = lockEvent(eventId);
			if (event.getStatus() == EventStatus.CANCELLED)
				return new ArrayList<Integer>(); // idempotent: nothing more to do
			if (event.getStatus() == EventStatus.COMPLETED || event.getStatus() == EventStatus.DELETED)
				throw new BusinessRuleViolationException("Event cannot be cancelled in status " + event.getStatus());
			if (event.getEndAt().isBefore(now()))
				throw new BusinessRuleViolationException("Event already ended, it cannot be cancelled!");

			event.setStatus(EventStatus.CANCELLED);
			cancelActiveHoldsOf(eventId);

			List<Integer> refundOrderIds = new ArrayList<>();
			for (Order order : orderRepository.findByEventIdAndStatus(eventId, OrderStatus.PAID)) {
				order.setStatus(OrderStatus.REFUNDED);
				refundOrderIds.add(order.getOrderId());
			}
			for (Ticket ticket : ticketRepository.findByEventId(eventId))
				if (ticket.getStatus() == TicketStatus.ISSUED)
					ticket.setStatus(TicketStatus.REFUNDED);
			return refundOrderIds;
		});
	}

	// (2) delete: only when nothing was sold (financial records must stay consistent)
	@Override
	public Integer deleteEvent(Integer eventId) {
		return transactionManager.inTransaction(() -> {
			Event event = lockEvent(eventId);
			boolean hasSoldSeats = event.getEventSeats().stream().anyMatch(s -> s.getStatus() == SeatStatus.SOLD);
			if (hasSoldSeats)
				throw new BusinessRuleViolationException("Event with sold tickets cannot be deleted, cancel it instead!");
			cancelActiveHoldsOf(eventId);
			eventRepository.remove(event); // DELETE event + ticket types + seats (cascade)
			return eventId;
		});
	}

	// (3) ONE transaction: expired holds released, SeatHold + items inserted, every requested EventSeat AVAILABLE -> HELD.
	//     The event row lock makes the check "is the seat available?" and the update "seat is HELD" atomic.
	@Override
	public Integer holdSeats(Integer userId, Integer eventId, List<Integer> eventSeatIds) {
		return transactionManager.inTransaction(() -> {
			LocalDateTime now = now();
			Event event = lockEvent(eventId);
			releaseExpiredHolds(eventId, now);

			UserAccount user = userRepository.getByIdOrNull(userId);
			if (user == null || !user.isActive())
				throw new BusinessRuleViolationException("Unknown or inactive user: " + userId);
			if (event.getStatus() != EventStatus.PUBLISHED)
				throw new BusinessRuleViolationException("Seats can be held only for PUBLISHED events, current: " + event.getStatus());
			if (eventSeatIds == null || eventSeatIds.isEmpty())
				throw new BusinessRuleViolationException("At least one seat must be selected!");

			// all-or-nothing: check every seat before changing any
			Set<Integer> distinctIds = new LinkedHashSet<>(eventSeatIds);
			if (seatHoldRepository.countSeatsOwnedByUser(userId, eventId) + distinctIds.size() > MAX_TICKETS_PER_USER)
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
			seatHoldRepository.add(hold); // the hold id is generated here ...
			for (EventSeat seat : seats)
				seat.hold(hold.getHoldId(), hold.getExpiresAt()); // ... and written on every held seat
			return hold.getHoldId();
		});
	}

	// (4) ONE transaction: the hold is re-checked against the seats it holds, then the order and its items are inserted
	//     (items copied from the held seats). A hold found expired is released and committed, then the failure is reported.
	@Override
	public Integer createOrder(Integer holdId) {
		return commitThenFailIfRequested(deferred -> {
			Event event = lockEventOfHold(holdId);
			LocalDateTime now = now();
			SeatHold hold = requireHold(holdId);
			if (hold.getStatus() == HoldStatus.ACTIVE && hold.isExpiredAt(now))
				expireHold(hold);
			if (hold.getStatus() != HoldStatus.ACTIVE) {
				deferred.set(new BusinessRuleViolationException("Seat hold is not active (status " + hold.getStatus() + ")!"));
				return null;
			}
			boolean orderExists = orderRepository.findByHoldId(holdId).stream().anyMatch(this::isUnpaid);
			if (orderExists)
				throw new BusinessRuleViolationException("An order already exists for hold " + holdId);

			// every seat of the hold must still be HELD by this very hold
			for (SeatHoldItem item : hold.getItems()) {
				EventSeat seat = event.findEventSeat(item.getEventSeatId());
				if (seat == null || seat.getStatus() != SeatStatus.HELD || !holdId.equals(seat.getHoldId()))
					throw new BusinessRuleViolationException("Seat hold " + holdId + " does not match the held seats of the event!");
			}

			Order order = entityFactory.buildOrder(hold, event);
			pricingService.computingTotal(order);
			validatingService.validateWithException(order);
			orderRepository.add(order); // INSERT order + order items (cascade)
			return order.getOrderId();
		});
	}

	// (5) the payment layer asks permission BEFORE charging the card
	@Override
	public Integer startPayment(Integer orderId) {
		return commitThenFailIfRequested(deferred -> {
			LocalDateTime now = now();
			lockEventOfOrder(orderId);
			Order order = requireOrder(orderId);
			if (order.getStatus() == OrderStatus.EXPIRED)
				throw new PaymentRejectedException();
			if (order.getStatus() != OrderStatus.PENDING)
				throw new BusinessRuleViolationException("Payment can start only for a PENDING order (status " + order.getStatus() + ")!");
			SeatHold hold = requireHold(order.getHoldId());
			if (hold.getStatus() == HoldStatus.ACTIVE && hold.isExpiredAt(now))
				expireHold(hold); // seats released, order EXPIRED: committed, then the payment is rejected
			if (hold.getStatus() != HoldStatus.ACTIVE) {
				deferred.set(new PaymentRejectedException());
				return null;
			}
			order.setStatus(OrderStatus.PAYMENT_PROCESSING); // seats stay held until the payment result arrives
			return order.getOrderId();
		});
	}

	// (5) payment success (webhook). ONE transaction: seats SOLD, hold CONVERTED, order PAID, tickets inserted.
	@Override
	public List<Integer> confirmPayment(Integer orderId) {
		return commitThenFailIfRequested(deferred -> {
			LocalDateTime now = now();
			Event event = lockEventOfOrder(orderId);
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
				deferred.set(new BusinessRuleViolationException("Seat hold expired before payment; the payment must be refunded!"));
				return null;
			}

			for (SeatHoldItem item : hold.getItems()) {
				EventSeat seat = event.findEventSeat(item.getEventSeatId());
				if (seat == null)
					throw new BusinessRuleViolationException("Unknown event seat: " + item.getEventSeatId());
				seat.sell();
			}
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
		});
	}

	// (5) payment failure (webhook)
	@Override
	public Integer failPayment(Integer orderId) {
		return transactionManager.inTransaction(() -> {
			lockEventOfOrder(orderId);
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
		});
	}

	// (6) periodic job: one transaction per event (the event row is locked while its expired holds are released)
	@Override
	public int expireHolds(LocalDateTime now) {
		int count = 0;
		for (Integer eventId : seatHoldRepository.findEventIdsWithExpiredActiveHolds(now)) {
			count += transactionManager.inTransaction(() -> {
				eventRepository.getByIdForUpdate(eventId); // null when the event no longer exists: holds are still expired
				return releaseExpiredHolds(eventId, now);
			});
		}
		return count;
	}

	// (7) ONE transaction: the ticket row is locked (FOR UPDATE), checked and turned into USED. Gates scanning the same
	//     QR code at the same moment are serialized by the database: the first one gets VALID, the others ALREADY_USED.
	@Override
	public TicketValidationView validateTicket(String qrToken, Integer eventId) {
		return transactionManager.inTransaction(() -> {
			Ticket ticket = ticketRepository.findByQrTokenForUpdate(qrToken);
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
				ticket.markUsed(now()); // UPDATE flushed at commit, while the row is still locked
				result = TicketScanResult.VALID;
			}
			return new TicketValidationView(result, ticket.getTicketId(), ticket.getSeatLabel(), ticket.getTicketTypeName());
		});
	}

	// (8)
	@Override
	public EventSalesView getEventSalesReport(Integer eventId) {
		return transactionManager.inTransaction(() -> {
			Event event = requireEvent(eventId);
			return consolidatingService.getEventSalesViewDataOf(event, ticketRepository.findByEventId(eventId));
		});
	}

	/* ------------------------- private helpers (run inside the transaction of the caller) ------------------------- */

	/**
	 * Runs {@code work} in a transaction that is COMMITTED even when the caller must be told "no": the work stores the
	 * failure in {@code deferred} (after it made the state changes that have to survive, e.g. the expiry of a hold) and
	 * the failure is thrown after the commit. A failure thrown directly by the work rolls everything back.
	 * (If this step is called inside a bigger transaction, the failure is thrown inside it.)
	 */
	private <T> T commitThenFailIfRequested(Function<AtomicReference<RuntimeException>, T> work) {
		AtomicReference<RuntimeException> deferred = new AtomicReference<>();
		T result = transactionManager.inTransaction(() -> work.apply(deferred));
		if (deferred.get() != null)
			throw deferred.get();
		return result;
	}

	/** Locks the event row (SELECT ... FOR UPDATE) and loads the aggregate with its committed state. */
	private Event lockEvent(Integer eventId) {
		Event event = eventRepository.getByIdForUpdate(eventId);
		if (event == null)
			throw new BusinessRuleViolationException("Event not found: " + eventId);
		return event;
	}

	/** Steps that start from a hold/order find its event with a scalar query, lock the event, and only then load the hold/order. */
	private Event lockEventOfHold(Integer holdId) {
		Integer eventId = seatHoldRepository.findEventIdByHoldId(holdId);
		if (eventId == null)
			throw new BusinessRuleViolationException("Seat hold not found: " + holdId);
		return lockEvent(eventId);
	}

	private Event lockEventOfOrder(Integer orderId) {
		Integer eventId = orderRepository.findEventIdByOrderId(orderId);
		if (eventId == null)
			throw new BusinessRuleViolationException("Order not found: " + orderId);
		return lockEvent(eventId);
	}

	private int releaseExpiredHolds(Integer eventId, LocalDateTime now) {
		int count = 0;
		for (SeatHold hold : seatHoldRepository.findExpiredActiveByEventId(eventId, now)) {
			if (!isPaymentInProgress(hold, now)) {
				expireHold(hold);
				count++;
			}
		}
		return count;
	}

	private void expireHold(SeatHold hold) {
		releaseHoldSeats(hold);
		hold.setStatus(HoldStatus.EXPIRED);
		for (Order order : orderRepository.findByHoldId(hold.getHoldId()))
			if (isUnpaid(order))
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
		for (Order order : orderRepository.findByHoldId(hold.getHoldId()))
			if (order.getStatus() == OrderStatus.PAYMENT_PROCESSING)
				return true;
		return false;
	}

	private void cancelActiveHoldsOf(Integer eventId) {
		for (SeatHold hold : seatHoldRepository.findByEventIdAndStatus(eventId, HoldStatus.ACTIVE)) {
			releaseHoldSeats(hold);
			hold.setStatus(HoldStatus.CANCELLED);
			for (Order order : orderRepository.findByHoldId(hold.getHoldId()))
				if (isUnpaid(order))
					order.setStatus(OrderStatus.CANCELLED);
		}
	}

	private void releaseHoldSeats(SeatHold hold) {
		Event event = eventRepository.getByIdOrNull(hold.getEventId()); // the instance already loaded (and locked) by this transaction
		if (event == null)
			return;
		for (SeatHoldItem item : hold.getItems()) {
			EventSeat seat = event.findEventSeat(item.getEventSeatId());
			if (seat != null && seat.getStatus() == SeatStatus.HELD && hold.getHoldId().equals(seat.getHoldId()))
				seat.release();
		}
	}

	private boolean hasSalesOrHolds(Event event) {
		return event.getEventSeats().stream()
				.anyMatch(s -> s.getStatus() == SeatStatus.SOLD || s.getStatus() == SeatStatus.HELD);
	}

	private Event requireEvent(Integer eventId) {
		Event event = eventRepository.getByIdOrNull(eventId);
		if (event == null)
			throw new BusinessRuleViolationException("Event not found: " + eventId);
		return event;
	}

	private SeatHold requireHold(Integer holdId) {
		SeatHold hold = seatHoldRepository.getByIdOrNull(holdId);
		if (hold == null)
			throw new BusinessRuleViolationException("Seat hold not found: " + holdId);
		return hold;
	}

	private Order requireOrder(Integer orderId) {
		Order order = orderRepository.getByIdOrNull(orderId);
		if (order == null)
			throw new BusinessRuleViolationException("Order not found: " + orderId);
		return order;
	}
}
