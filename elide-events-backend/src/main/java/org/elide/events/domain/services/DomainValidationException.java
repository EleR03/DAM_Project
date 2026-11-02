package org.elide.events.domain.services;

/** Thrown when JavaBean Validation / aggregate integrity rules are violated. */
public class DomainValidationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public DomainValidationException(String message) {
		super(message);
	}
}
