package org.elide.events.domain.sales;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/** Aggregate root: purchase order created from a seat hold. The total is a computed field. */
@Entity
@Table(name = "orders")
public class Order implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue
	@NotNull(message = "OrderId is required!")
	private Integer orderId;

	@NotNull(message = "Order user is required!")
	private Integer userId;

	@NotNull(message = "Order event is required!")
	private Integer eventId;

	private Integer holdId;

	@Enumerated(EnumType.STRING)
	private OrderStatus status = OrderStatus.PENDING;

	@Valid
	@NotEmpty(message = "Order must contain at least one item!")
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	private String currency = "RON";
	private LocalDateTime createdAt;
	private LocalDateTime expiresAt;
	private LocalDateTime paidAt;

	/* computed by PricingOrderDomainService */
	@NotNull @DecimalMin("0.00")
	private BigDecimal total = BigDecimal.ZERO;

	@Transient
	private Integer itemCount = 0;

	public Order() {
		super();
	}

	public Order(Integer orderId, Integer userId, Integer eventId, Integer holdId,
			LocalDateTime createdAt, LocalDateTime expiresAt) {
		super();
		this.orderId = orderId;
		this.userId = userId;
		this.eventId = eventId;
		this.holdId = holdId;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public OrderItem addItem(Integer eventSeatId, String seatLabel, String ticketTypeName, BigDecimal unitPrice) {
		OrderItem item = new OrderItem(items.size() + 1, eventSeatId, seatLabel, ticketTypeName, unitPrice);
		items.add(item);
		return item;
	}

	public Integer getOrderId() { return orderId; }
	public void setOrderId(Integer orderId) { this.orderId = orderId; }
	public Integer getUserId() { return userId; }
	public void setUserId(Integer userId) { this.userId = userId; }
	public Integer getEventId() { return eventId; }
	public void setEventId(Integer eventId) { this.eventId = eventId; }
	public Integer getHoldId() { return holdId; }
	public void setHoldId(Integer holdId) { this.holdId = holdId; }
	public OrderStatus getStatus() { return status; }
	public void setStatus(OrderStatus status) { this.status = status; }
	public List<OrderItem> getItems() { return items; }
	public void setItems(List<OrderItem> items) { this.items = items; }
	public String getCurrency() { return currency; }
	public void setCurrency(String currency) { this.currency = currency; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public LocalDateTime getExpiresAt() { return expiresAt; }
	public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
	public LocalDateTime getPaidAt() { return paidAt; }
	public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
	public BigDecimal getTotal() { return total; }
	public void setTotal(BigDecimal total) { this.total = total; }
	public Integer getItemCount() { return itemCount; }
	public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }

	@Override
	public String toString() {
		return "Order [orderId=" + orderId + ", eventId=" + eventId + ", status=" + status
				+ ", items=" + items.size() + ", total=" + total + " " + currency + "]";
	}
}
