package org.elide.events.domain.sales;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/** Component entity of the SeatHold aggregate. */
@Entity
public class SeatHoldItem implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue
	private Integer itemId;
	private Integer eventSeatId;

	// back-reference to the aggregate root (owning side of the SeatHold.items relationship)
	@ManyToOne
	private SeatHold hold;

	public SeatHoldItem() {
		super();
	}

	public SeatHoldItem(Integer itemId, Integer eventSeatId) {
		super();
		this.itemId = itemId;
		this.eventSeatId = eventSeatId;
	}

	public SeatHold getHold() { return hold; }
	public void setHold(SeatHold hold) { this.hold = hold; }
	public Integer getItemId() { return itemId; }
	public void setItemId(Integer itemId) { this.itemId = itemId; }
	public Integer getEventSeatId() { return eventSeatId; }
	public void setEventSeatId(Integer eventSeatId) { this.eventSeatId = eventSeatId; }
}
