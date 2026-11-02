package org.elide.events.domain.sales;

/** Domain Entity View: result of a QR scan. */
public class TicketValidationView {
	private final TicketScanResult result;
	private final Integer ticketId;
	private final String seatLabel;
	private final String ticketTypeName;

	public TicketValidationView(TicketScanResult result, Integer ticketId, String seatLabel, String ticketTypeName) {
		super();
		this.result = result;
		this.ticketId = ticketId;
		this.seatLabel = seatLabel;
		this.ticketTypeName = ticketTypeName;
	}

	public boolean isValid() { return TicketScanResult.VALID.equals(result); }
	public TicketScanResult getResult() { return result; }
	public Integer getTicketId() { return ticketId; }
	public String getSeatLabel() { return seatLabel; }
	public String getTicketTypeName() { return ticketTypeName; }

	@Override
	public String toString() {
		return "TicketValidationView [result=" + result + ", ticketId=" + ticketId + ", seat=" + seatLabel
				+ ", type=" + ticketTypeName + "]";
	}
}
