package org.elide.events.domain.services;

import org.elide.events.domain.event.Event;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

// SDI Bean Component
@Component @Scope("singleton")
public class EventRepositoryBase extends InMemoryEntityRepositoryBase<Event> implements IEventRepository {
	@Override
	protected Integer idOf(Event entity) {
		return entity.getEventId();
	}

	@Override
	protected void assignId(Event entity, Integer id) {
		entity.setEventId(id);
	}
}
