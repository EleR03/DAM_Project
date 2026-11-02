package org.elide.events.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.elide.events.domain.event.EventStatus;
import org.elide.events.domain.event.SeatStatus;
import org.elide.events.domain.identity.UserAccount;
import org.elide.events.domain.identity.UserRole;
import org.elide.events.domain.sales.HoldStatus;
import org.elide.events.domain.sales.OrderStatus;
import org.elide.events.domain.sales.TicketScanResult;
import org.elide.events.domain.sales.TicketStatus;
import org.junit.jupiter.api.Test;

/** CANCEL (operational, always possible before COMPLETED) vs DELETE (only without sold tickets). */
public class Test6_EventCancellationTest extends AbstractDomainTest {

	/* ---------------- CANCEL ---------------- */

	@Test
	public void eventWithSoldTicketsCanBeCancelledAndTriggersTheCompensationFlow() {
		Integer eventId = newPublishedEvent(3, 4);
		UserAccount buyerA = newUser(UserRole.PARTICIPANT);
		UserAccount buyerB = newUser(UserRole.PARTICIPANT);
		UserAccount buyerC = newUser(UserRole.PARTICIPANT);

		// A: paid order (2 tickets); B: hold + pending order; C: hold only
		Integer orderA = workflow.createOrder(workflow.holdSeats(buyerA.getUserId(), eventId, seats(1, 2)));
		List<Integer> ticketIds = workflow.confirmPayment(orderA);
		String qr = ticketRepository.getById(ticketIds.get(0)).getQrToken();
		Integer holdB = workflow.holdSeats(buyerB.getUserId(), eventId, seats(3));
		Integer orderB = workflow.createOrder(holdB);
		Integer holdC = workflow.holdSeats(buyerC.getUserId(), eventId, seats(4));

		List<Integer> refunds = workflow.cancelEvent(eventId);

		// 1. the event is CANCELLED (not blocked by the sold tickets)
		assertEquals(EventStatus.CANCELLED, event(eventId).getStatus());
		// 2. held seats and active holds are released immediately
		assertEquals(SeatStatus.AVAILABLE, event(eventId).findEventSeat(3).getStatus());
		assertEquals(SeatStatus.AVAILABLE, event(eventId).findEventSeat(4).getStatus());
		assertEquals(HoldStatus.CANCELLED, seatHoldRepository.getById(holdB).getStatus());
		assertEquals(HoldStatus.CANCELLED, seatHoldRepository.getById(holdC).getStatus());
		assertEquals(OrderStatus.CANCELLED, orderRepository.getById(orderB).getStatus());
		// 3. issued tickets are REFUNDED and the scanner rejects them
		for (Integer ticketId : ticketIds)
			assertEquals(TicketStatus.REFUNDED, ticketRepository.getById(ticketId).getStatus());
		assertEquals(TicketScanResult.CANCELLED, workflow.validateTicket(qr, eventId).getResult());
		// 4. refund trigger: exactly the PAID orders are returned for the payment layer
		assertEquals(1, refunds.size());
		assertTrue(refunds.contains(orderA));
		assertEquals(OrderStatus.REFUNDED, orderRepository.getById(orderA).getStatus());
	}

	@Test
	public void everyPaidOrderOfTheEventIsReturnedForRefund() {
		Integer eventId = newPublishedEvent(2, 5);
		UserAccount buyerA = newUser(UserRole.PARTICIPANT);
		UserAccount buyerB = newUser(UserRole.PARTICIPANT);
		Integer orderA = workflow.createOrder(workflow.holdSeats(buyerA.getUserId(), eventId, seats(1)));
		Integer orderB = workflow.createOrder(workflow.holdSeats(buyerB.getUserId(), eventId, seats(2, 3)));
		workflow.confirmPayment(orderA);
		workflow.confirmPayment(orderB);

		List<Integer> refunds = workflow.cancelEvent(eventId);

		assertEquals(new HashSet<>(java.util.Arrays.asList(orderA, orderB)), new HashSet<>(refunds));
		assertEquals(3, ticketRepository.findByEventId(eventId).stream()
				.filter(t -> t.getStatus() == TicketStatus.REFUNDED).count());
	}

	@Test
	public void ordersOfOtherEventsAreNotTouched() {
		Integer eventId = newPublishedEvent(1, 4);
		Integer otherEventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer otherOrder = workflow.createOrder(workflow.holdSeats(buyer.getUserId(), otherEventId, seats(1)));
		workflow.confirmPayment(otherOrder);

		workflow.cancelEvent(eventId);

		assertEquals(OrderStatus.PAID, orderRepository.getById(otherOrder).getStatus());
		assertEquals(EventStatus.PUBLISHED, event(otherEventId).getStatus());
	}

	@Test
	public void noNewSeatCanBeHeldOnACancelledEvent() {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(1));

		workflow.cancelEvent(eventId);

		assertThrows(BusinessRuleViolationException.class,
				() -> workflow.holdSeats(buyer.getUserId(), eventId, seats(2)));
		assertThrows(BusinessRuleViolationException.class, () -> workflow.createOrder(holdId));
	}

	@Test
	public void cancellingTwiceIsIdempotent() {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer orderId = workflow.createOrder(workflow.holdSeats(buyer.getUserId(), eventId, seats(1)));
		workflow.confirmPayment(orderId);

		assertEquals(1, workflow.cancelEvent(eventId).size());
		assertTrue(workflow.cancelEvent(eventId).isEmpty()); // no second refund
		assertEquals(EventStatus.CANCELLED, event(eventId).getStatus());
		assertEquals(OrderStatus.REFUNDED, orderRepository.getById(orderId).getStatus());
	}

	@Test
	public void draftEventCanBeCancelledToo() {
		Integer eventId = newDraftEvent(1, 4);
		assertTrue(workflow.cancelEvent(eventId).isEmpty());
		assertEquals(EventStatus.CANCELLED, event(eventId).getStatus());
	}

	@Test
	public void completedEventCannotBeCancelled() {
		Integer eventId = newPublishedEvent(1, 4);
		event(eventId).setStatus(EventStatus.COMPLETED);
		assertThrows(BusinessRuleViolationException.class, () -> workflow.cancelEvent(eventId));
		assertEquals(EventStatus.COMPLETED, event(eventId).getStatus());
	}

	@Test
	public void eventThatAlreadyEndedCannotBeCancelled() {
		Integer eventId = newPublishedEvent(1, 4);
		event(eventId).setStartAt(inDays(-2));
		event(eventId).setEndAt(inDays(-2).plusHours(3));
		assertThrows(BusinessRuleViolationException.class, () -> workflow.cancelEvent(eventId));
		assertEquals(EventStatus.PUBLISHED, event(eventId).getStatus());
	}

	/* ---------------- DELETE ---------------- */

	@Test
	public void eventWithoutSalesCanBeDeletedAndItsHoldsAreReleased() {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(1));

		workflow.deleteEvent(eventId);

		assertNull(eventRepository.getById(eventId));
		assertEquals(HoldStatus.CANCELLED, seatHoldRepository.getById(holdId).getStatus());
	}

	@Test
	public void draftEventCanBeDeleted() {
		Integer eventId = newDraftEvent(1, 4);
		workflow.deleteEvent(eventId);
		assertNull(eventRepository.getById(eventId));
	}

	@Test
	public void eventWithSoldTicketsCannotBeDeletedNotEvenAfterCancel() {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer orderId = workflow.createOrder(workflow.holdSeats(buyer.getUserId(), eventId, seats(1)));
		workflow.confirmPayment(orderId);

		assertThrows(BusinessRuleViolationException.class, () -> workflow.deleteEvent(eventId));
		workflow.cancelEvent(eventId);
		// the financial records stay: cancel is allowed, delete is not
		assertThrows(BusinessRuleViolationException.class, () -> workflow.deleteEvent(eventId));
		assertEquals(EventStatus.CANCELLED, event(eventId).getStatus());
	}
}
