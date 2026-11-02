package org.elide.events.domain.services;

import org.elide.events.domain.sales.Order;

/** Domain Computation Service: computed Order fields. */
public interface IPricingOrderDomainService {
	/** total = sum(unitPrice) of the order items; also fills itemCount. */
	Order computingTotal(Order order);
}
