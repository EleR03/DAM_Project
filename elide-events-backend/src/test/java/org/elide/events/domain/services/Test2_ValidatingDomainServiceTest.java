package org.elide.events.domain.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Set;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.sales.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Domain Validation Service: JavaBean Validation rules + aggregate rules. */
public class Test2_ValidatingDomainServiceTest extends AbstractDomainTest {

	@Autowired
	private IValidatingEventDomainService validator;

	private Event event;

	@BeforeEach
	public void setUp() {
		event = factory.buildEventWithSeats(1, newHall(2, 5), "Concert Valid",
				inDays(10), inDays(10).plusHours(2), new BigDecimal("80.00"));
	}

	private boolean hasViolationOn(Set<String> violations, String text) {
		return violations.stream().anyMatch(v -> v.contains(text));
	}

	@Test
	public void validEventHasNoViolations() {
		assertTrue(validator.validate(event).isEmpty());
		assertDoesNotThrow(() -> validator.validateWithException(event));
		assertTrue(validator.validateEventAggregate(event));
	}

	@Test
	public void titleIsRequired() {
		event.setTitle(null);
		assertTrue(hasViolationOn(validator.validate(event), "title"));
	}

	@Test
	public void titleMustHaveAtLeast3Characters() {
		event.setTitle("ab");
		assertTrue(hasViolationOn(validator.validate(event), "title"));
	}

	@Test
	public void startDateMustBeInTheFuture() {
		event.setStartAt(inDays(-1));
		event.setEndAt(inDays(-1).plusHours(2));
		assertTrue(hasViolationOn(validator.validate(event), "startAt"));
	}

	@Test
	public void endMustBeAfterStart() {
		event.setEndAt(event.getStartAt().minusHours(1));
		assertTrue(hasViolationOn(validator.validate(event), "End date must be after start date"));
	}

	@Test
	public void basePriceMustNotBeNegative() {
		event.setBasePrice(new BigDecimal("-1.00"));
		assertTrue(hasViolationOn(validator.validate(event), "basePrice"));
	}

	@Test
	public void componentTicketTypeIsValidatedByCascade() {
		event.getTicketTypes().get(0).setName(" ");
		assertTrue(hasViolationOn(validator.validate(event), "Ticket type name is required"));
	}

	@Test
	public void invalidEventThrowsDomainValidationException() {
		event.setTitle(null);
		assertThrows(DomainValidationException.class, () -> validator.validateWithException(event));
	}

	@Test
	public void aggregateWithoutSeatsIsRejected() {
		event.setEventSeats(new ArrayList<>());
		assertTrue(hasViolationOn(validator.validate(event), "at least one seat"));
		assertThrows(DomainValidationException.class, () -> validator.validateEventAggregate(event));
	}

	@Test
	public void orderWithoutItemsIsInvalid() {
		Order order = new Order(1, 1, 1, 1, inDays(0), inDays(0).plusMinutes(10));
		assertTrue(hasViolationOn(validator.validate(order), "at least one item"));
		assertThrows(DomainValidationException.class, () -> validator.validateWithException(order));
	}
}
