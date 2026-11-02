package org.elide.events.domain.sales;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/** Entity: issued ticket. The QR token is a random secret, validated only by the backend. */
@Entity
public class Ticket implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id @GeneratedValue
	private Integer ticketId;
	private Integer orderId;
	private Integer eventId;
	private Integer userId;
	private Integer eventSeatId;
	private String seatLabel;
	private String ticketTypeName;
	private String ticketCode;
	private String qrToken;

	@Enumerated(EnumType.STRING)
	private TicketStatus status = TicketStatus.ISSUED;

	private LocalDateTime issuedAt;
	private LocalDateTime usedAt;

	public Ticket() {
		super();
	}

	public void markUsed(LocalDateTime when) {
		if (status != TicketStatus.ISSUED)
			throw new IllegalStateException("Ticket " + ticketCode + " cannot be USED from status " + status);
		this.status = TicketStatus.USED;
		this.usedAt = when;
	}

	public Integer getTicketId() { return ticketId; }
	public void setTicketId(Integer ticketId) { this.ticketId = ticketId; }
	public Integer getOrderId() { return orderId; }
	public void setOrderId(Integer orderId) { this.orderId = orderId; }
	public Integer getEventId() { return eventId; }
	public void setEventId(Integer eventId) { this.eventId = eventId; }
	public Integer getUserId() { return userId; }
	public void setUserId(Integer userId) { this.userId = userId; }
	public Integer getEventSeatId() { return eventSeatId; }
	public void setEventSeatId(Integer eventSeatId) { this.eventSeatId = eventSeatId; }
	public String getSeatLabel() { return seatLabel; }
	public void setSeatLabel(String seatLabel) { this.seatLabel = seatLabel; }
	public String getTicketTypeName() { return ticketTypeName; }
	public void setTicketTypeName(String ticketTypeName) { this.ticketTypeName = ticketTypeName; }
	public String getTicketCode() { return ticketCode; }
	public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }
	public String getQrToken() { return qrToken; }
	public void setQrToken(String qrToken) { this.qrToken = qrToken; }
	public TicketStatus getStatus() { return status; }
	public void setStatus(TicketStatus status) { this.status = status; }
	public LocalDateTime getIssuedAt() { return issuedAt; }
	public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
	public LocalDateTime getUsedAt() { return usedAt; }
	public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }

	@Override
	public String toString() {
		return "Ticket [ticketId=" + ticketId + ", code=" + ticketCode + ", seat=" + seatLabel + ", status=" + status + "]";
	}
}
