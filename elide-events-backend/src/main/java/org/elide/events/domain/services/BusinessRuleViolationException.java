package org.elide.events.domain.services;

/** Thrown when a business workflow rule is violated (wrong status, unknown entity, not allowed action...). */
public class BusinessRuleViolationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public BusinessRuleViolationException(String message) {
		super(message);
	}
}
