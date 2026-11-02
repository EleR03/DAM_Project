package org.elide.events.domain.services;

import java.math.BigDecimal;
import java.util.Collection;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.event.EventSalesView;
import org.elide.events.domain.event.EventSeat;
import org.elide.events.domain.event.SeatStatus;
import org.elide.events.domain.sales.Ticket;
import org.elide.events.domain.sales.TicketStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConsolidatingEventSalesViewDomainServiceImpl implements IConsolidatingEventSalesViewDomainService {

	@Autowired
	private ISummarizingEventDomainService summarizingService;

	@Override
	public EventSalesView getEventSalesViewDataOf(Event event, Collection<Ticket> eventTickets) {
		summarizingService.computingOccupancy(event);

		BigDecimal grossRevenue = BigDecimal.ZERO.setScale(2);
		for (EventSeat seat : event.getEventSeats())
			if (seat.getStatus() == SeatStatus.SOLD)
				grossRevenue = grossRevenue.add(seat.getPrice());

		int scanned = (int) eventTickets.stream().filter(t -> t.getStatus() == TicketStatus.USED).count();

		return new EventSalesView(event.getEventId(), event.getTitle(), event.getCapacity(),
				event.getAvailableCount(), event.getHeldCount(), event.getSoldCount(),
				scanned, event.getSoldCount() - scanned, grossRevenue, event.getOccupancyRate());
	}
}
