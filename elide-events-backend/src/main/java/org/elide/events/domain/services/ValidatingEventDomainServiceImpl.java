package org.elide.events.domain.services;

import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.event.EventSeat;
import org.elide.events.domain.sales.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ValidatingEventDomainServiceImpl implements IValidatingEventDomainService {
	private static Logger logger = Logger.getLogger(ValidatingEventDomainServiceImpl.class.getName());

	@Autowired
	private Validator validator;

	@Override
	public Set<String> validate(Event event) {
		return toMessages(validator.validate(event));
	}

	@Override
	public void validateWithException(Event event) {
		Set<String> violations = validate(event);
		if (!violations.isEmpty())
			throw new DomainValidationException("Event validation failed: " + String.join("; ", violations));
	}

	@Override
	public boolean validateEventAggregate(Event event) {
		if (event == null)
			throw new DomainValidationException("Null Event!");
		if (event.getHall() == null)
			throw new DomainValidationException("Event has no hall!");
		if (event.getEventSeats() == null || event.getEventSeats().isEmpty())
			throw new DomainValidationException("Event has no seats!");
		if (event.getTicketTypes() == null || event.getTicketTypes().isEmpty())
			throw new DomainValidationException("Event has no ticket types!");
		for (EventSeat seat : event.getEventSeats()) {
			if (seat.getTicketType() == null || seat.getPrice() == null)
				throw new DomainValidationException("Seat " + seat.getLabel() + " has no ticket type or price!");
		}
		return true;
	}

	@Override
	public Set<String> validate(Order order) {
		return toMessages(validator.validate(order));
	}

	@Override
	public void validateWithException(Order order) {
		Set<String> violations = validate(order);
		if (!violations.isEmpty())
			throw new DomainValidationException("Order validation failed: " + String.join("; ", violations));
	}

	private <T> Set<String> toMessages(Set<ConstraintViolation<T>> violations) {
		logger.info("Violations count: " + violations.size());
		return violations.stream()
				.map(v -> v.getPropertyPath() + ": " + v.getMessage())
				.collect(Collectors.toSet());
	}
}
