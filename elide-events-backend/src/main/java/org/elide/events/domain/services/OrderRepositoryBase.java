package org.elide.events.domain.services;

import org.elide.events.domain.sales.Order;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

// SDI Bean Component
@Component @Scope("singleton")
public class OrderRepositoryBase extends InMemoryEntityRepositoryBase<Order> implements IOrderRepository {
	@Override
	protected Integer idOf(Order entity) {
		return entity.getOrderId();
	}

	@Override
	protected void assignId(Order entity, Integer id) {
		entity.setOrderId(id);
	}
}
