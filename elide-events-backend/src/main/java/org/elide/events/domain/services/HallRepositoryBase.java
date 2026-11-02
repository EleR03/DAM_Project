package org.elide.events.domain.services;

import org.elide.events.domain.venue.Hall;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

// SDI Bean Component
@Component @Scope("singleton")
public class HallRepositoryBase extends InMemoryEntityRepositoryBase<Hall> implements IHallRepository {
	@Override
	protected Integer idOf(Hall entity) {
		return entity.getHallId();
	}

	@Override
	protected void assignId(Hall entity, Integer id) {
		entity.setHallId(id);
	}
}
