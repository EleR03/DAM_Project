package org.elide.events.domain.services;

import java.util.Set;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.sales.Order;

/** Domain Validation Service: entity + aggregate integrity rules. */
public interface IValidatingEventDomainService {
	/** JavaBean Validation of the Event aggregate (cascaded to ticket types). */
	Set<String> validate(Event event);

	void validateWithException(Event event);

	/** Aggregate rules: hall, seats, ticket types, every seat has a ticket type and a price. */
	boolean validateEventAggregate(Event event);

	Set<String> validate(Order order);

	void validateWithException(Order order);
}
