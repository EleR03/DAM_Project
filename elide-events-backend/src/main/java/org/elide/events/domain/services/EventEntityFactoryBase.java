package org.elide.events.domain.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.event.EventSeat;
import org.elide.events.domain.event.TicketType;
import org.elide.events.domain.identity.UserAccount;
import org.elide.events.domain.identity.UserRole;
import org.elide.events.domain.sales.Order;
import org.elide.events.domain.sales.OrderItem;
import org.elide.events.domain.sales.SeatHold;
import org.elide.events.domain.sales.Ticket;
import org.elide.events.domain.venue.Hall;
import org.elide.events.domain.venue.Seat;
import org.elide.events.domain.venue.Venue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("EventEntityFactoryBase")
@Scope("singleton")
public class EventEntityFactoryBase implements IEventEntityFactory {
	private static Logger logger = Logger.getLogger(EventEntityFactoryBase.class.getName());

	public static final String DEFAULT_TICKET_TYPE = "STANDARD";

	@Autowired private IUserAccountRepository userRepository;
	@Autowired private IHallRepository hallRepository;
	@Autowired private IEventRepository eventRepository;
	@Autowired private ISeatHoldRepository seatHoldRepository;
	@Autowired private IOrderRepository orderRepository;
	@Autowired private ITicketRepository ticketRepository;

	private final AtomicInteger venueIds = new AtomicInteger(0);

	public EventEntityFactoryBase() {
		logger.info(">>> BEAN: EventEntityFactoryBase instantiated!");
	}

	@Override
	public UserAccount buildUser(String email, String firstName, String lastName, UserRole role) {
		return new UserAccount(userRepository.getNextID(), email, firstName, lastName, role);
	}

	@Override
	public Venue buildVenue(String name, String city) {
		return new Venue(venueIds.incrementAndGet(), name, city);
	}

	@Override
	public Hall buildHall(Venue venue, String name, int rows, int seatsPerRow) {
		if (rows < 1 || rows > 26 || seatsPerRow < 1)
			throw new IllegalArgumentException("Hall needs 1..26 rows and at least 1 seat per row!");
		Hall hall = new Hall(hallRepository.getNextID(), name, venue);
		for (int r = 0; r < rows; r++) {
			String rowLabel = String.valueOf((char) ('A' + r));
			for (int n = 1; n <= seatsPerRow; n++)
				hall.addSeat(rowLabel, n);
		}
		return hall;
	}

	@Override
	public Event buildEventWithSeats(Integer organizerId, Hall hall, String title,
			LocalDateTime startAt, LocalDateTime endAt, BigDecimal basePrice) {
		Event event = new Event(eventRepository.getNextID(), organizerId, title, startAt, endAt, basePrice, hall);
		TicketType standard = event.addTicketType(DEFAULT_TICKET_TYPE, basePrice);
		for (Seat seat : hall.getSeats())
			if (seat.isActive())
				event.addEventSeat(seat.getSeatId(), seat.getRowLabel(), seat.getLabel(), standard);
		return event;
	}

	@Override
	public SeatHold buildSeatHold(Integer userId, Event event, Collection<Integer> eventSeatIds,
			LocalDateTime createdAt, int holdMinutes) {
		SeatHold hold = new SeatHold(seatHoldRepository.getNextID(), userId, event.getEventId(),
				createdAt, createdAt.plusMinutes(holdMinutes));
		for (Integer eventSeatId : eventSeatIds)
			hold.addItem(eventSeatId);
		return hold;
	}

	@Override
	public Order buildOrder(SeatHold hold, Event event) {
		Order order = new Order(orderRepository.getNextID(), hold.getUserId(), event.getEventId(),
				hold.getHoldId(), hold.getCreatedAt(), hold.getExpiresAt());
		order.setCurrency(event.getCurrency());
		hold.getItems().forEach(item -> {
			EventSeat seat = event.findEventSeat(item.getEventSeatId());
			order.addItem(seat.getEventSeatId(), seat.getLabel(), seat.getTicketType().getName(), seat.getPrice());
		});
		return order;
	}

	@Override
	public Ticket buildTicket(Order order, OrderItem item, LocalDateTime issuedAt) {
		Ticket ticket = new Ticket();
		ticket.setTicketId(ticketRepository.getNextID());
		ticket.setOrderId(order.getOrderId());
		ticket.setEventId(order.getEventId());
		ticket.setUserId(order.getUserId());
		ticket.setEventSeatId(item.getEventSeatId());
		ticket.setSeatLabel(item.getSeatLabel());
		ticket.setTicketTypeName(item.getTicketTypeName());
		ticket.setTicketCode("TKT-" + order.getEventId() + "-" + ticket.getTicketId());
		ticket.setQrToken(UUID.randomUUID().toString()); // random, unguessable (SecureRandom-backed)
		ticket.setIssuedAt(issuedAt);
		return ticket;
	}
}
