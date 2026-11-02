package org.elide.events.domain.services;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.elide.events.domain.sales.Order;
import org.elide.events.domain.sales.OrderItem;
import org.springframework.stereotype.Service;

@Service
public class PricingOrderDomainServiceImpl implements IPricingOrderDomainService {

	@Override
	public Order computingTotal(Order order) {
		BigDecimal total = BigDecimal.ZERO;
		for (OrderItem item : order.getItems())
			total = total.add(item.getUnitPrice());
		order.setItemCount(order.getItems().size());
		order.setTotal(total.setScale(2, RoundingMode.HALF_UP));
		return order;
	}
}
