package org.elide.events.domain.services;

/** Equivalent of HTTP 409 SEAT_NO_LONGER_AVAILABLE: the seat was taken by someone else. */
public class SeatNoLongerAvailableException extends BusinessRuleViolationException {
	private static final long serialVersionUID = 1L;

	public SeatNoLongerAvailableException(String seatLabel) {
		super("SEAT_NO_LONGER_AVAILABLE: " + seatLabel);
	}
}
