package org.elide.events.domain.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Component entity of the Event aggregate: a hall seat as it exists for ONE event.
 * State rules (State pattern light): AVAILABLE -> HELD -> SOLD, HELD -> AVAILABLE, AVAILABLE -> BLOCKED.
 */
@Entity
public class EventSeat implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue
	private Integer eventSeatId;
	private Integer seatId;      // reference to Hall.Seat
	private String rowLabel;
	private String label;        // e.g. A12

	@Enumerated(EnumType.STRING)
	private SeatStatus status = SeatStatus.AVAILABLE;

	private BigDecimal price;

	@ManyToOne
	private TicketType ticketType;

	private Integer holdId;
	private LocalDateTime heldUntil;

	public EventSeat() {
		super();
	}

	public EventSeat(Integer eventSeatId, Integer seatId, String rowLabel, String label,
			TicketType ticketType, BigDecimal price) {
		super();
		this.eventSeatId = eventSeatId;
		this.seatId = seatId;
		this.rowLabel = rowLabel;
		this.label = label;
		this.ticketType = ticketType;
		this.price = price;
	}

	public boolean isAvailable() {
		return SeatStatus.AVAILABLE.equals(status);
	}

	public void hold(Integer holdId, LocalDateTime until) {
		if (status != SeatStatus.AVAILABLE)
			throw new IllegalStateException("Seat " + label + " cannot be HELD from status " + status);
		this.status = SeatStatus.HELD;
		this.holdId = holdId;
		this.heldUntil = until;
	}

	public void release() {
		if (status != SeatStatus.HELD)
			throw new IllegalStateException("Seat " + label + " cannot be released from status " + status);
		this.status = SeatStatus.AVAILABLE;
		this.holdId = null;
		this.heldUntil = null;
	}

	public void sell() {
		if (status != SeatStatus.HELD)
			throw new IllegalStateException("Seat " + label + " cannot be SOLD from status " + status);
		this.status = SeatStatus.SOLD;
		this.holdId = null;
		this.heldUntil = null;
	}

	public void block() {
		if (status != SeatStatus.AVAILABLE)
			throw new IllegalStateException("Seat " + label + " cannot be BLOCKED from status " + status);
		this.status = SeatStatus.BLOCKED;
	}

	public Integer getEventSeatId() { return eventSeatId; }
	public void setEventSeatId(Integer eventSeatId) { this.eventSeatId = eventSeatId; }
	public Integer getSeatId() { return seatId; }
	public void setSeatId(Integer seatId) { this.seatId = seatId; }
	public String getRowLabel() { return rowLabel; }
	public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }
	public String getLabel() { return label; }
	public void setLabel(String label) { this.label = label; }
	public SeatStatus getStatus() { return status; }
	public BigDecimal getPrice() { return price; }
	public void setPrice(BigDecimal price) { this.price = price; }
	public TicketType getTicketType() { return ticketType; }
	public void setTicketType(TicketType ticketType) { this.ticketType = ticketType; }
	public Integer getHoldId() { return holdId; }
	public LocalDateTime getHeldUntil() { return heldUntil; }

	@Override
	public String toString() {
		return "EventSeat [" + label + ", " + status + ", " + price + "]";
	}
}
