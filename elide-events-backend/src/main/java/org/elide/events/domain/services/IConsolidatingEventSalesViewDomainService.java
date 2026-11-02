package org.elide.events.domain.services;

import java.util.Collection;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.event.EventSalesView;
import org.elide.events.domain.sales.Ticket;

/** Domain Computation Service: computed Entity View (organizer report for one event). */
public interface IConsolidatingEventSalesViewDomainService {
	EventSalesView getEventSalesViewDataOf(Event event, Collection<Ticket> eventTickets);
}
