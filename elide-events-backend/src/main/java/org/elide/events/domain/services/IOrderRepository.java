package org.elide.events.domain.services;

import java.util.List;

import org.elide.events.domain.sales.Order;
import org.elide.events.domain.sales.OrderStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IOrderRepository extends IEntityRepository<Order> {
	List<Order> findByHoldId(Integer holdId);

	List<Order> findByEventId(Integer eventId);

	List<Order> findByEventIdAndStatus(Integer eventId, OrderStatus status);

	List<Order> findByUserId(Integer userId);

	/** Scalar lookup (nothing is loaded): the event of an order, or null. Used to lock the event before touching the order. */
	@Query("select o.eventId from SalesOrder o where o.orderId = :orderId")
	Integer findEventIdByOrderId(@Param("orderId") Integer orderId);
}
