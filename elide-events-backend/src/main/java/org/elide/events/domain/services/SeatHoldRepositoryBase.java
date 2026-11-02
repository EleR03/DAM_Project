package org.elide.events.domain.services;

import org.elide.events.domain.sales.SeatHold;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

// SDI Bean Component
@Component @Scope("singleton")
public class SeatHoldRepositoryBase extends InMemoryEntityRepositoryBase<SeatHold> implements ISeatHoldRepository {
	@Override
	protected Integer idOf(SeatHold entity) {
		return entity.getHoldId();
	}

	@Override
	protected void assignId(SeatHold entity, Integer id) {
		entity.setHoldId(id);
	}
}
