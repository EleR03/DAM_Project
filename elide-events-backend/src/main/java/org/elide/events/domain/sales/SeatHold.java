package org.elide.events.domain.sales;

import java.io.Serializable;
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

/** Aggregate root: temporary reservation (10 minutes) of a set of event seats. */
@Entity
public class SeatHold implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue
	private Integer holdId;
	private Integer userId;
	private Integer eventId;
	private LocalDateTime createdAt;
	private LocalDateTime expiresAt;

	@Enumerated(EnumType.STRING)
	private HoldStatus status = HoldStatus.ACTIVE;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SeatHoldItem> items = new ArrayList<>();

	public SeatHold() {
		super();
	}

	public SeatHold(Integer holdId, Integer userId, Integer eventId, LocalDateTime createdAt, LocalDateTime expiresAt) {
		super();
		this.holdId = holdId;
		this.userId = userId;
		this.eventId = eventId;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public SeatHoldItem addItem(Integer eventSeatId) {
		SeatHoldItem item = new SeatHoldItem(items.size() + 1, eventSeatId);
		items.add(item);
		return item;
	}

	public boolean isExpiredAt(LocalDateTime now) {
		return now.isAfter(expiresAt);
	}

	public Integer getHoldId() { return holdId; }
	public void setHoldId(Integer holdId) { this.holdId = holdId; }
	public Integer getUserId() { return userId; }
	public void setUserId(Integer userId) { this.userId = userId; }
	public Integer getEventId() { return eventId; }
	public void setEventId(Integer eventId) { this.eventId = eventId; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public LocalDateTime getExpiresAt() { return expiresAt; }
	public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
	public HoldStatus getStatus() { return status; }
	public void setStatus(HoldStatus status) { this.status = status; }
	public List<SeatHoldItem> getItems() { return items; }
	public void setItems(List<SeatHoldItem> items) { this.items = items; }

	@Override
	public String toString() {
		return "SeatHold [holdId=" + holdId + ", eventId=" + eventId + ", status=" + status
				+ ", expiresAt=" + expiresAt + ", seats=" + items.size() + "]";
	}
}
