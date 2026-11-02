package org.elide.events.domain.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.identity.UserAccount;
import org.elide.events.domain.identity.UserRole;
import org.elide.events.domain.sales.Order;
import org.elide.events.domain.sales.OrderItem;
import org.elide.events.domain.sales.SeatHold;
import org.elide.events.domain.sales.Ticket;
import org.elide.events.domain.venue.Hall;
import org.elide.events.domain.venue.Venue;

/** Entity/Aggregate factory: creates entities with new identities and assembled components. */
public interface IEventEntityFactory {

	UserAccount buildUser(String email, String firstName, String lastName, UserRole role);

	Venue buildVenue(String name, String city);

	/** Hall with rows x seatsPerRow seats, rows labelled A, B, C ... (max 26 rows). */
	Hall buildHall(Venue venue, String name, int rows, int seatsPerRow);

	/** DRAFT event with a STANDARD ticket type (price = basePrice) and one EventSeat per active hall seat. */
	Event buildEventWithSeats(Integer organizerId, Hall hall, String title,
			LocalDateTime startAt, LocalDateTime endAt, BigDecimal basePrice);

	SeatHold buildSeatHold(Integer userId, Event event, Collection<Integer> eventSeatIds,
			LocalDateTime createdAt, int holdMinutes);

	/** Order (items copied from the held seats, total NOT yet computed). */
	Order buildOrder(SeatHold hold, Event event);

	Ticket buildTicket(Order order, OrderItem item, LocalDateTime issuedAt);
}
