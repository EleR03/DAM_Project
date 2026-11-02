package org.elide.events.domain.services;

import org.elide.events.domain.event.Event;

/** Domain Computation Service: computed Event fields. */
public interface ISummarizingEventDomainService {
	/** Fills availableCount, heldCount, soldCount, blockedCount. */
	Event countingSeats(Event event);

	/** occupancyRate = soldCount / capacity * 100 (2 decimals, HALF_UP). */
	Event computingOccupancy(Event event);
}
