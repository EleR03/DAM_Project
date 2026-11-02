package org.elide.events.domain.event;

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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.elide.events.domain.venue.Hall;

/**
 * Aggregate root: Event.
 * Components: TicketType, EventSeat (entities) + enums (value objects).
 */
@Entity
public class Event implements Serializable, Comparable<Event> {
	private static final long serialVersionUID = 1L;

	/* ---------- internal structure: member fields ---------- */
	@Id @GeneratedValue
	@NotNull(message = "EventId is required!")
	private Integer eventId;

	@NotNull(message = "Organizer is required!")
	private Integer organizerId;

	@NotBlank(message = "Event title is required!")
	@Size(min = 3, max = 200, message = "Event title must have between 3 and 200 characters!")
	private String title;

	private String description;

	@Enumerated(EnumType.STRING)
	private EventStatus status = EventStatus.DRAFT;

	@NotNull(message = "StartAt is required!")
	@Future(message = "StartAt must be a future date!")
	private LocalDateTime startAt;

	@NotNull(message = "EndAt is required!")
	private LocalDateTime endAt;

	private LocalDateTime publishedAt;

	@NotNull(message = "Base price is required!")
	@DecimalMin(value = "0.00", message = "Base price must not be negative!")
	private BigDecimal basePrice;

	private String currency = "RON";

	@ManyToOne
	private Hall hall;

	@Valid
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	private List<TicketType> ticketTypes = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	private List<EventSeat> eventSeats = new ArrayList<>();

	/* ---------- computed fields: transient, filled by SummarizingEventDomainService ---------- */
	@Transient private Integer availableCount = 0;
	@Transient private Integer heldCount = 0;
	@Transient private Integer soldCount = 0;
	@Transient private Integer blockedCount = 0;
	@Transient private BigDecimal occupancyRate = BigDecimal.ZERO;

	/* ---------- constructors ---------- */
	public Event() {
		super();
	}

	public Event(Integer eventId, Integer organizerId, String title, LocalDateTime startAt,
			LocalDateTime endAt, BigDecimal basePrice, Hall hall) {
		super();
		this.eventId = eventId;
		this.organizerId = organizerId;
		this.title = title;
		this.startAt = startAt;
		this.endAt = endAt;
		this.basePrice = basePrice;
		this.hall = hall;
	}

	/* ---------- aggregate behaviour: the root controls its components ---------- */
	public TicketType addTicketType(String name, BigDecimal price) {
		TicketType ticketType = new TicketType(ticketTypes.size() + 1, name, price);
		ticketTypes.add(ticketType);
		return ticketType;
	}

	public EventSeat addEventSeat(Integer seatId, String rowLabel, String label, TicketType ticketType) {
		EventSeat eventSeat = new EventSeat(eventSeats.size() + 1, seatId, rowLabel, label,
				ticketType, ticketType.getPrice());
		eventSeats.add(eventSeat);
		return eventSeat;
	}

	/** Price a whole row with a ticket type (e.g. row A = VIP). */
	public void assignTicketTypeToRow(String rowLabel, TicketType ticketType) {
		for (EventSeat eventSeat : eventSeats) {
			if (eventSeat.getRowLabel().equalsIgnoreCase(rowLabel)) {
				eventSeat.setTicketType(ticketType);
				eventSeat.setPrice(ticketType.getPrice());
			}
		}
	}

	public EventSeat findEventSeat(Integer eventSeatId) {
		return eventSeats.stream()
				.filter(s -> s.getEventSeatId().equals(eventSeatId))
				.findFirst().orElse(null);
	}

	public TicketType findTicketType(String name) {
		return ticketTypes.stream()
				.filter(t -> t.getName().equalsIgnoreCase(name))
				.findFirst().orElse(null);
	}

	/** Computed: capacity = number of sellable seats (BLOCKED seats are out of sale). */
	public int getCapacity() {
		return (int) eventSeats.stream().filter(s -> s.getStatus() != SeatStatus.BLOCKED).count();
	}

	/* ---------- JavaBean Validation: integrity rules ---------- */
	@AssertTrue(message = "End date must be after start date!")
	public boolean isScheduleValid() {
		if (startAt == null || endAt == null)
			return true; // reported by @NotNull
		return endAt.isAfter(startAt);
	}

	@AssertTrue(message = "Event must have a hall, at least one seat and at least one ticket type!")
	public boolean isStructureValid() {
		return hall != null
				&& eventSeats != null && !eventSeats.isEmpty()
				&& ticketTypes != null && !ticketTypes.isEmpty();
	}

	/* ---------- accessors ---------- */
	public Integer getEventId() { return eventId; }
	public void setEventId(Integer eventId) { this.eventId = eventId; }
	public Integer getOrganizerId() { return organizerId; }
	public void setOrganizerId(Integer organizerId) { this.organizerId = organizerId; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public EventStatus getStatus() { return status; }
	public void setStatus(EventStatus status) { this.status = status; }
	public LocalDateTime getStartAt() { return startAt; }
	public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
	public LocalDateTime getEndAt() { return endAt; }
	public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
	public LocalDateTime getPublishedAt() { return publishedAt; }
	public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
	public BigDecimal getBasePrice() { return basePrice; }
	public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
	public String getCurrency() { return currency; }
	public void setCurrency(String currency) { this.currency = currency; }
	public Hall getHall() { return hall; }
	public void setHall(Hall hall) { this.hall = hall; }
	public List<TicketType> getTicketTypes() { return ticketTypes; }
	public void setTicketTypes(List<TicketType> ticketTypes) { this.ticketTypes = ticketTypes; }
	public List<EventSeat> getEventSeats() { return eventSeats; }
	public void setEventSeats(List<EventSeat> eventSeats) { this.eventSeats = eventSeats; }

	public Integer getAvailableCount() { return availableCount; }
	public void setAvailableCount(Integer availableCount) { this.availableCount = availableCount; }
	public Integer getHeldCount() { return heldCount; }
	public void setHeldCount(Integer heldCount) { this.heldCount = heldCount; }
	public Integer getSoldCount() { return soldCount; }
	public void setSoldCount(Integer soldCount) { this.soldCount = soldCount; }
	public Integer getBlockedCount() { return blockedCount; }
	public void setBlockedCount(Integer blockedCount) { this.blockedCount = blockedCount; }
	public BigDecimal getOccupancyRate() { return occupancyRate; }
	public void setOccupancyRate(BigDecimal occupancyRate) { this.occupancyRate = occupancyRate; }

	@Override
	public int hashCode() {
		return (eventId == null) ? 0 : eventId.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Event other = (Event) obj;
		return eventId == null ? other.eventId == null : eventId.equals(other.eventId);
	}

	@Override
	public int compareTo(Event other) {
		return this.eventId.compareTo(other.eventId);
	}

	@Override
	public String toString() {
		return "Event [eventId=" + eventId + ", title=" + title + ", status=" + status
				+ ", startAt=" + startAt + ", seats=" + eventSeats.size() + "]";
	}
}
