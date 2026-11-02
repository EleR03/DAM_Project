package org.elide.events.domain.venue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** Aggregate root: a hall of a venue; it owns its seats. */
@Entity
public class Hall implements Serializable, Comparable<Hall> {
	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue
	@NotNull(message = "HallId is required!")
	private Integer hallId;

	@NotBlank(message = "Hall name is required!")
	private String name;

	@ManyToOne
	private Venue venue;

	@Valid
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Seat> seats = new ArrayList<>();

	public Hall() {
		super();
	}

	public Hall(Integer hallId, String name, Venue venue) {
		super();
		this.hallId = hallId;
		this.name = name;
		this.venue = venue;
	}

	/** The root controls how components are added (seat identity is local to the hall). */
	public Seat addSeat(String rowLabel, int seatNumber) {
		Seat seat = new Seat(seats.size() + 1, rowLabel, seatNumber);
		seats.add(seat);
		return seat;
	}

	/** Computed: number of active seats. */
	public int getCapacity() {
		return (int) seats.stream().filter(Seat::isActive).count();
	}

	public Integer getHallId() { return hallId; }
	public void setHallId(Integer hallId) { this.hallId = hallId; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public Venue getVenue() { return venue; }
	public void setVenue(Venue venue) { this.venue = venue; }
	public List<Seat> getSeats() { return seats; }
	public void setSeats(List<Seat> seats) { this.seats = seats; }

	@Override
	public int hashCode() {
		return (hallId == null) ? 0 : hallId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Hall other = (Hall) obj;
		return hallId == null ? other.hallId == null : hallId.equals(other.hallId);
	}

	@Override
	public int compareTo(Hall other) {
		return this.hallId.compareTo(other.hallId);
	}

	@Override
	public String toString() {
		return "Hall [hallId=" + hallId + ", name=" + name + ", capacity=" + getCapacity() + "]";
	}
}
