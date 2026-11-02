package org.elide.events.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.event.SeatStatus;
import org.elide.events.domain.identity.UserAccount;
import org.elide.events.domain.identity.UserRole;
import org.junit.jupiter.api.Test;

/** Business rule: a user can hold/buy at most MAX_TICKETS_PER_USER tickets for one event. */
public class Test7_TicketLimitTest extends AbstractDomainTest {

	private static final int MAX = EventTicketingBusinessWorkflowServiceImpl.MAX_TICKETS_PER_USER;

	/** Seat ids from..from+count-1 */
	private List<Integer> seatRange(int from, int count) {
		List<Integer> ids = new ArrayList<>();
		for (int i = 0; i < count; i++)
			ids.add(from + i);
		return ids;
	}

	private long heldSeats(Integer eventId) {
		Event event = event(eventId);
		return event.getEventSeats().stream().filter(s -> s.getStatus() == SeatStatus.HELD).count();
	}

	@Test
	public void exactlyTheMaximumIsAllowed() {
		Integer eventId = newPublishedEvent(5, 10);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);

		workflow.holdSeats(buyer.getUserId(), eventId, seatRange(1, MAX));

		assertEquals(MAX, heldSeats(eventId));
	}

	@Test
	public void moreThanTheMaximumInOneRequestIsRejectedAndNothingIsHeld() {
		Integer eventId = newPublishedEvent(5, 10);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);

		TicketLimitExceededException ex = assertThrows(TicketLimitExceededException.class,
				() -> workflow.holdSeats(buyer.getUserId(), eventId, seatRange(1, MAX + 1)));

		assertTrue(ex.getMessage().contains("TICKET_LIMIT_EXCEEDED"));
		assertEquals(0, heldSeats(eventId));
	}

	@Test
	public void theLimitIsCumulativeAcrossHolds() {
		Integer eventId = newPublishedEvent(5, 10);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		workflow.holdSeats(buyer.getUserId(), eventId, seatRange(1, MAX - 2));

		assertThrows(TicketLimitExceededException.class,
				() -> workflow.holdSeats(buyer.getUserId(), eventId, seatRange(20, 3)));
		workflow.holdSeats(buyer.getUserId(), eventId, seatRange(20, 2)); // reaches exactly MAX

		assertEquals(MAX, heldSeats(eventId));
	}

	@Test
	public void alreadyBoughtTicketsCountTowardTheLimit() {
		Integer eventId = newPublishedEvent(5, 10);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer orderId = workflow.createOrder(workflow.holdSeats(buyer.getUserId(), eventId, seatRange(1, MAX)));
		workflow.confirmPayment(orderId);

		assertThrows(TicketLimitExceededException.class,
				() -> workflow.holdSeats(buyer.getUserId(), eventId, seats(30)));
	}

	@Test
	public void releasedSeatsFreeTheQuotaAgain() {
		Integer eventId = newPublishedEvent(5, 10);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer orderId = workflow.createOrder(workflow.holdSeats(buyer.getUserId(), eventId, seatRange(1, MAX)));

		workflow.failPayment(orderId); // payment failed: seats released
		workflow.holdSeats(buyer.getUserId(), eventId, seatRange(1, MAX)); // allowed again

		assertEquals(MAX, heldSeats(eventId));
	}

	@Test
	public void expiredHoldFreesTheQuotaAgain() {
		Integer eventId = newPublishedEvent(5, 10);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		workflow.holdSeats(buyer.getUserId(), eventId, seatRange(1, MAX));

		workflow.expireHolds(java.time.LocalDateTime.now().plusMinutes(11));
		workflow.holdSeats(buyer.getUserId(), eventId, seatRange(1, MAX));

		assertEquals(MAX, heldSeats(eventId));
	}

	@Test
	public void theLimitIsPerEventAndPerUser() {
		Integer eventId = newPublishedEvent(5, 10);
		Integer otherEventId = newPublishedEvent(5, 10);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		UserAccount otherBuyer = newUser(UserRole.PARTICIPANT);
		workflow.holdSeats(buyer.getUserId(), eventId, seatRange(1, MAX));

		// same user, another event: allowed; another user, same event: allowed
		workflow.holdSeats(buyer.getUserId(), otherEventId, seatRange(1, MAX));
		workflow.holdSeats(otherBuyer.getUserId(), eventId, seatRange(MAX + 1, MAX));

		assertEquals(2 * MAX, heldSeats(eventId));
	}

	@Test
	public void twentyThreadsOfTheSameUserNeverExceedTheLimit() throws Exception {
		Integer eventId = newPublishedEvent(5, 10); // 50 seats, plenty available
		UserAccount buyer = newUser(UserRole.PARTICIPANT);

		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			Integer seatId = i + 1;
			tasks.add(() -> {
				try {
					workflow.holdSeats(buyer.getUserId(), eventId, seats(seatId));
					return true;
				} catch (TicketLimitExceededException ex) {
					return false;
				}
			});
		}
		List<Boolean> results = race(tasks);

		assertEquals(MAX, results.stream().filter(r -> r).count());
		assertEquals(MAX, heldSeats(eventId));
	}
}
