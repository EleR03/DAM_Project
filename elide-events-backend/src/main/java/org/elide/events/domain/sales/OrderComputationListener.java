package org.elide.events.domain.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

/**
 * JPA entity listener (computation rule): the stored order total is always the sum of the item prices,
 * recomputed right before the order is inserted or updated (same rule as PricingOrderDomainService,
 * applied at the persistence boundary so that no code path can store an inconsistent total).
 */
public class OrderComputationListener {

	@PrePersist
	@PreUpdate
	public void computeTotal(Order order) {
		BigDecimal total = BigDecimal.ZERO;
		if (order.getItems() != null) {
			for (OrderItem item : order.getItems())
				if (item.getUnitPrice() != null)
					total = total.add(item.getUnitPrice());
			order.setItemCount(order.getItems().size());
		}
		order.setTotal(total.setScale(2, RoundingMode.HALF_UP));
	}
}
