package org.elide.events.domain.services;

import java.util.Collection;
import java.util.stream.Collectors;

import org.elide.events.domain.sales.Ticket;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

// SDI Bean Component
@Component @Scope("singleton")
public class TicketRepositoryBase extends InMemoryEntityRepositoryBase<Ticket> implements ITicketRepository {
	@Override
	protected Integer idOf(Ticket entity) {
		return entity.getTicketId();
	}

	@Override
	protected void assignId(Ticket entity, Integer id) {
		entity.setTicketId(id);
	}

	@Override
	public Ticket findByQrToken(String qrToken) {
		if (qrToken == null)
			return null;
		return toCollection().stream()
				.filter(t -> qrToken.equals(t.getQrToken()))
				.findFirst().orElse(null);
	}

	@Override
	public Collection<Ticket> findByEventId(Integer eventId) {
		return toCollection().stream()
				.filter(t -> t.getEventId().equals(eventId))
				.collect(Collectors.toList());
	}
}
