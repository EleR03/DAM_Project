package org.elide.events.domain.sales;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

/** Component entity of the Order aggregate: one seat in the order. */
@Entity
public class OrderItem implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue
	private Integer orderItemId;

	@NotNull(message = "Order item must reference an event seat!")
	private Integer eventSeatId;
	private String seatLabel;
	private String ticketTypeName;

	@NotNull(message = "Unit price is required!")
	@DecimalMin(value = "0.00", message = "Unit price must not be negative!")
	private BigDecimal unitPrice;

	public OrderItem() {
		super();
	}

	public OrderItem(Integer orderItemId, Integer eventSeatId, String seatLabel,
			String ticketTypeName, BigDecimal unitPrice) {
		super();
		this.orderItemId = orderItemId;
		this.eventSeatId = eventSeatId;
		this.seatLabel = seatLabel;
		this.ticketTypeName = ticketTypeName;
		this.unitPrice = unitPrice;
	}

	public Integer getOrderItemId() { return orderItemId; }
	public void setOrderItemId(Integer orderItemId) { this.orderItemId = orderItemId; }
	public Integer getEventSeatId() { return eventSeatId; }
	public void setEventSeatId(Integer eventSeatId) { this.eventSeatId = eventSeatId; }
	public String getSeatLabel() { return seatLabel; }
	public void setSeatLabel(String seatLabel) { this.seatLabel = seatLabel; }
	public String getTicketTypeName() { return ticketTypeName; }
	public void setTicketTypeName(String ticketTypeName) { this.ticketTypeName = ticketTypeName; }
	public BigDecimal getUnitPrice() { return unitPrice; }
	public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
