package org.elide.events.domain.services;

import java.util.Collection;

import org.elide.events.domain.sales.Ticket;

public interface ITicketRepository extends IEntityRepository<Ticket> {
	Ticket findByQrToken(String qrToken);

	Collection<Ticket> findByEventId(Integer eventId);
}
