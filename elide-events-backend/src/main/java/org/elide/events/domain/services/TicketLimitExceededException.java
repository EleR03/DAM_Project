package org.elide.events.domain.services;

/** A user tried to hold/buy more than the maximum number of tickets allowed per event. */
public class TicketLimitExceededException extends BusinessRuleViolationException {
	private static final long serialVersionUID = 1L;

	public TicketLimitExceededException(int maxTickets) {
		super("TICKET_LIMIT_EXCEEDED: at most " + maxTickets + " tickets per user for one event");
	}
}
