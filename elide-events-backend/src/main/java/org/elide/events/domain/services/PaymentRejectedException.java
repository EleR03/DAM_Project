package org.elide.events.domain.services;

/** The payment is refused BEFORE charging the card (e.g. the 10-minute seat hold expired): no money is taken. */
public class PaymentRejectedException extends BusinessRuleViolationException {
	private static final long serialVersionUID = 1L;

	public PaymentRejectedException() {
		super("PAYMENT_REJECTED: the seat hold expired, the card is not charged");
	}
}
