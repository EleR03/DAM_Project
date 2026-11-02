package org.elide.events.domain.venue;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/** Component entity of the Hall aggregate: a physical seat (identity is local to the hall). */
@Entity
public class Seat implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue
	private Integer seatId;
	private String rowLabel;   // A, B, C ...
	private int seatNumber;    // 1, 2, 3 ...
	private boolean accessible = false;
	private boolean active = true;

	public Seat() {
		super();
	}

	public Seat(Integer seatId, String rowLabel, int seatNumber) {
		super();
		this.seatId = seatId;
		this.rowLabel = rowLabel;
		this.seatNumber = seatNumber;
	}

	/** Computed value, e.g. "A12". */
	public String getLabel() {
		return rowLabel + seatNumber;
	}

	public Integer getSeatId() { return seatId; }
	public void setSeatId(Integer seatId) { this.seatId = seatId; }
	public String getRowLabel() { return rowLabel; }
	public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }
	public int getSeatNumber() { return seatNumber; }
	public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }
	public boolean isAccessible() { return accessible; }
	public void setAccessible(boolean accessible) { this.accessible = accessible; }
	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }

	@Override
	public String toString() {
		return "Seat [" + getLabel() + "]";
	}
}
