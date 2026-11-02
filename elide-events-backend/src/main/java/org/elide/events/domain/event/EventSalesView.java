package org.elide.events.domain.event;

import java.math.BigDecimal;

/** Domain Entity View (tuple): consolidated sales data of one event, for organizer reports. */
public class EventSalesView {
	private Integer eventId;
	private String title;
	private Integer capacity;
	private Integer availableSeats;
	private Integer heldSeats;
	private Integer soldSeats;
	private Integer scannedTickets;
	private Integer notAttended;
	private BigDecimal grossRevenue;
	private BigDecimal occupancyRate; // percent, 2 decimals

	public EventSalesView(Integer eventId, String title, Integer capacity, Integer availableSeats,
			Integer heldSeats, Integer soldSeats, Integer scannedTickets, Integer notAttended,
			BigDecimal grossRevenue, BigDecimal occupancyRate) {
		super();
		this.eventId = eventId;
		this.title = title;
		this.capacity = capacity;
		this.availableSeats = availableSeats;
		this.heldSeats = heldSeats;
		this.soldSeats = soldSeats;
		this.scannedTickets = scannedTickets;
		this.notAttended = notAttended;
		this.grossRevenue = grossRevenue;
		this.occupancyRate = occupancyRate;
	}

	public Integer getEventId() { return eventId; }
	public String getTitle() { return title; }
	public Integer getCapacity() { return capacity; }
	public Integer getAvailableSeats() { return availableSeats; }
	public Integer getHeldSeats() { return heldSeats; }
	public Integer getSoldSeats() { return soldSeats; }
	public Integer getScannedTickets() { return scannedTickets; }
	public Integer getNotAttended() { return notAttended; }
	public BigDecimal getGrossRevenue() { return grossRevenue; }
	public BigDecimal getOccupancyRate() { return occupancyRate; }

	@Override
	public String toString() {
		return "EventSalesView [eventId=" + eventId + ", title=" + title + ", capacity=" + capacity
				+ ", available=" + availableSeats + ", held=" + heldSeats + ", sold=" + soldSeats
				+ ", scanned=" + scannedTickets + ", notAttended=" + notAttended
				+ ", grossRevenue=" + grossRevenue + ", occupancyRate=" + occupancyRate + "%]";
	}
}
