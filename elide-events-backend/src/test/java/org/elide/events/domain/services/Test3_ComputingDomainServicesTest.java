package org.elide.events.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.event.EventSalesView;
import org.elide.events.domain.sales.Order;
import org.elide.events.domain.sales.Ticket;
import org.elide.events.domain.sales.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Domain Computation Services: computed entity fields and computed entity views. */
public class Test3_ComputingDomainServicesTest extends AbstractDomainTest {

	@Autowired private ISummarizingEventDomainService summarizingService;
	@Autowired private IPricingOrderDomainService pricingService;
	@Autowired private IConsolidatingEventSalesViewDomainService consolidatingService;

	private Event event; // 3 rows x 4 seats = 12 seats, 100.00 each

	@BeforeEach
	public void setUp() {
		event = factory.buildEventWithSeats(1, newHall(3, 4), "Concert Computing",
				inDays(10), inDays(10).plusHours(2), new BigDecimal("100.00"));
	}

	@Test
	public void countingSeatsFillsComputedFields() {
		event.getEventSeats().get(0).hold(1, inDays(1));
		event.getEventSeats().get(0).sell();                 // 1 SOLD
		event.getEventSeats().get(1).hold(1, inDays(1));     // 1 HELD
		event.getEventSeats().get(2).block();                // 1 BLOCKED

		summarizingService.countingSeats(event);

		assertEquals(1, event.getSoldCount());
		assertEquals(1, event.getHeldCount());
		assertEquals(1, event.getBlockedCount());
		assertEquals(9, event.getAvailableCount());
	}

	@Test
	public void occupancyIsSoldOverCapacityInPercent() {
		for (int i = 0; i < 3; i++) {
			event.getEventSeats().get(i).hold(1, inDays(1));
			event.getEventSeats().get(i).sell();
		}
		summarizingService.computingOccupancy(event);
		assertEquals(new BigDecimal("25.00"), event.getOccupancyRate()); // 3 / 12 * 100
	}

	@Test
	public void occupancyIsRoundedToTwoDecimals() {
		event.getEventSeats().get(0).hold(1, inDays(1));
		event.getEventSeats().get(0).sell();
		summarizingService.computingOccupancy(event);
		assertEquals(new BigDecimal("8.33"), event.getOccupancyRate()); // 1 / 12 * 100
	}

	@Test
	public void orderTotalIsTheSumOfItemPrices() {
		Order order = new Order(1, 1, 1, 1, inDays(0), inDays(0).plusMinutes(10));
		order.addItem(1, "A1", "STANDARD", new BigDecimal("100.00"));
		order.addItem(2, "A2", "STANDARD", new BigDecimal("100.00"));

		pricingService.computingTotal(order);

		assertEquals(new BigDecimal("200.00"), order.getTotal());
		assertEquals(2, order.getItemCount());
	}

	@Test
	public void salesViewConsolidatesSeatsRevenueAndScans() {
		for (int i = 0; i < 4; i++) {
			event.getEventSeats().get(i).hold(1, inDays(1));
			event.getEventSeats().get(i).sell();
		}
		List<Ticket> tickets = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			Ticket t = new Ticket();
			t.setTicketId(i + 1);
			t.setEventId(event.getEventId());
			tickets.add(t);
		}
		tickets.get(0).setStatus(TicketStatus.USED);
		tickets.get(1).setStatus(TicketStatus.USED);

		EventSalesView view = consolidatingService.getEventSalesViewDataOf(event, tickets);

		assertEquals(12, view.getCapacity());
		assertEquals(4, view.getSoldSeats());
		assertEquals(8, view.getAvailableSeats());
		assertEquals(2, view.getScannedTickets());
		assertEquals(2, view.getNotAttended());
		assertEquals(new BigDecimal("400.00"), view.getGrossRevenue());
		assertEquals(new BigDecimal("33.33"), view.getOccupancyRate());
	}
}
