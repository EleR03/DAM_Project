package org.elide.events.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.event.EventSeat;
import org.elide.events.domain.event.SeatStatus;
import org.elide.events.domain.event.TicketType;
import org.elide.events.domain.venue.Hall;
import org.junit.jupiter.api.Test;

/** Entity Support Services: aggregate entity factory. */
public class Test1_EntityFactoryTest extends AbstractDomainTest {

	@Test
	public void hallIsBuiltWithRowsAndSeats() {
		Hall hall = factory.buildHall(factory.buildVenue("Teatrul National", "Iasi"), "Sala 1", 3, 4);
		assertEquals(12, hall.getCapacity());
		assertEquals("A1", hall.getSeats().get(0).getLabel());
		assertEquals("C4", hall.getSeats().get(11).getLabel());
	}

	@Test
	public void eventAggregateIsAssembledWithSeatsAndDefaultTicketType() {
		Hall hall = factory.buildHall(factory.buildVenue("Teatrul National", "Iasi"), "Sala 1", 3, 4);
		Event event = factory.buildEventWithSeats(1, hall, "Concert", inDays(10), inDays(10).plusHours(2),
				new BigDecimal("100.00"));

		assertNotNull(event.getEventId());
		assertEquals(12, event.getCapacity());
		assertEquals(1, event.getTicketTypes().size());
		assertEquals("STANDARD", event.getTicketTypes().get(0).getName());
		assertTrue(event.getEventSeats().stream().allMatch(s -> s.getStatus() == SeatStatus.AVAILABLE));
	}

	@Test
	public void rowCanBePricedWithAnotherTicketType() {
		Hall hall = factory.buildHall(factory.buildVenue("Teatrul National", "Iasi"), "Sala 1", 2, 3);
		Event event = factory.buildEventWithSeats(1, hall, "Concert", inDays(10), inDays(10).plusHours(2),
				new BigDecimal("100.00"));
		TicketType vip = event.addTicketType("VIP", new BigDecimal("250.00"));
		event.assignTicketTypeToRow("A", vip);

		EventSeat a1 = event.getEventSeats().get(0);
		EventSeat b1 = event.getEventSeats().get(3);
		assertEquals(new BigDecimal("250.00"), a1.getPrice());
		assertEquals("VIP", a1.getTicketType().getName());
		assertEquals(new BigDecimal("100.00"), b1.getPrice());
	}

	@Test
	public void identitiesAreUniquePerRepository() {
		Integer first = factory.buildUser("a@elide.test", "A", "A", org.elide.events.domain.identity.UserRole.PARTICIPANT).getUserId();
		Integer second = factory.buildUser("b@elide.test", "B", "B", org.elide.events.domain.identity.UserRole.PARTICIPANT).getUserId();
		assertTrue(second > first);
	}
}
