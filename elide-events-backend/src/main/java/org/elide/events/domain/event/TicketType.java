package org.elide.events.domain.event;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** Component entity of the Event aggregate (identity is local to the event), e.g. STANDARD, VIP. */
@Entity
public class TicketType implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue
	private Integer ticketTypeId;

	@NotBlank(message = "Ticket type name is required!")
	private String name;

	@NotNull(message = "Ticket type price is required!")
	@DecimalMin(value = "0.00", message = "Ticket type price must not be negative!")
	private BigDecimal price;

	public TicketType() {
		super();
	}

	public TicketType(Integer ticketTypeId, String name, BigDecimal price) {
		super();
		this.ticketTypeId = ticketTypeId;
		this.name = name;
		this.price = price;
	}

	public Integer getTicketTypeId() { return ticketTypeId; }
	public void setTicketTypeId(Integer ticketTypeId) { this.ticketTypeId = ticketTypeId; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public BigDecimal getPrice() { return price; }
	public void setPrice(BigDecimal price) { this.price = price; }

	@Override
	public String toString() {
		return "TicketType [" + name + ", " + price + "]";
	}
}
