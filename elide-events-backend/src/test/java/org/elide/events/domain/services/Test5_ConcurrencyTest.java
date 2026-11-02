package org.elide.events.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.event.EventSalesView;
import org.elide.events.domain.event.EventSeat;
import org.elide.events.domain.event.SeatStatus;
import org.elide.events.domain.identity.UserAccount;
import org.elide.events.domain.identity.UserRole;
import org.elide.events.domain.sales.HoldStatus;
import org.elide.events.domain.sales.SeatHold;
import org.elide.events.domain.sales.Ticket;
import org.elide.events.domain.sales.TicketScanResult;
import org.elide.events.domain.sales.TicketStatus;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Concurrency tests: no overselling, no seat sold or held twice.
 * All threads wait on one start latch and are released at the same moment.
 */
public class Test5_ConcurrencyTest extends AbstractDomainTest {
	private static Logger logger = Logger.getLogger(Test5_ConcurrencyTest.class.getName());

	private List<UserAccount> newParticipants(int count) {
		List<UserAccount> users = new ArrayList<>();
		for (int i = 0; i < count; i++)
			users.add(newUser(UserRole.PARTICIPANT));
		return users;
	}

	private long count(List<Boolean> results) {
		return results.stream().filter(r -> r).count();
	}

	private long activeHoldsOf(Integer eventId) {
		return seatHoldRepository.toCollection().stream()
				.filter(h -> h.getEventId().equals(eventId) && h.getStatus() == HoldStatus.ACTIVE)
				.count();
	}

	/* 200 threads fight for ONE seat: exactly one wins, 199 get SEAT_NO_LONGER_AVAILABLE. */
	@Test
	public void twoHundredThreadsOnTheSameSeatOnlyOneWins() throws Exception {
		Integer eventId = newPublishedEvent(1, 1);
		List<UserAccount> users = newParticipants(200);

		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (UserAccount user : users) {
			tasks.add(() -> {
				try {
					workflow.holdSeats(user.getUserId(), eventId, seats(1));
					return true;
				} catch (SeatNoLongerAvailableException ex) {
					return false;
				}
			});
		}
		List<Boolean> results = race(tasks);

		assertEquals(1, count(results));
		assertEquals(SeatStatus.HELD, event(eventId).findEventSeat(1).getStatus());
		assertEquals(1, activeHoldsOf(eventId));
	}

	/* 500 requests for the last 50 seats (10 requests per seat), full purchase for every winner: exactly 50 succeed. */
	@Test
	public void fiveHundredRequestsForTheLastFiftySeatsExactlyFiftySucceed() throws Exception {
		Integer eventId = newPublishedEvent(5, 10); // 50 seats x 100.00
		List<UserAccount> users = newParticipants(500);

		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (int i = 0; i < users.size(); i++) {
			Integer userId = users.get(i).getUserId();
			Integer seatId = (i % 50) + 1;
			tasks.add(() -> {
				try {
					Integer holdId = workflow.holdSeats(userId, eventId, seats(seatId));
					Integer orderId = workflow.createOrder(holdId);
					workflow.confirmPayment(orderId);
					return true;
				} catch (SeatNoLongerAvailableException ex) {
					return false;
				}
			});
		}
		List<Boolean> results = race(tasks);

		assertEquals(50, count(results));

		Event event = event(eventId);
		assertEquals(50, event.getEventSeats().stream().filter(s -> s.getStatus() == SeatStatus.SOLD).count());
		assertEquals(0, event.getEventSeats().stream().filter(s -> s.getStatus() != SeatStatus.SOLD).count());

		// one ticket per seat, no seat sold twice
		List<Ticket> tickets = new ArrayList<>(ticketRepository.findByEventId(eventId));
		assertEquals(50, tickets.size());
		Set<Integer> ticketedSeats = tickets.stream().map(Ticket::getEventSeatId).collect(Collectors.toSet());
		assertEquals(50, ticketedSeats.size());

		EventSalesView report = workflow.getEventSalesReport(eventId);
		assertEquals(50, report.getSoldSeats());
		assertEquals(0, report.getHeldSeats());
		assertTrue(report.getSoldSeats() + report.getHeldSeats() <= report.getCapacity());
		assertEquals(new BigDecimal("5000.00"), report.getGrossRevenue());
		assertEquals(new BigDecimal("100.00"), report.getOccupancyRate());
	}

	/*
	 * 8 seats left, 200 threads: 199 users ask for ONE seat each, one user asks for 5 seats at once (1..5).
	 * The 5-seat request is all-or-nothing: it either gets all 5 seats, or none (never a partial hold).
	 */
	@RepeatedTest(5)
	public void oneUserTakingFiveOfEightSeatsIsAllOrNothing() throws Exception {
		Integer eventId = newPublishedEvent(1, 8);
		List<UserAccount> users = newParticipants(200);
		Integer bigUserId = users.get(0).getUserId();

		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (int i = 0; i < users.size(); i++) {
			Integer userId = users.get(i).getUserId();
			List<Integer> wanted = (i == 0) ? seats(1, 2, 3, 4, 5) : seats((i % 8) + 1);
			tasks.add(() -> {
				try {
					workflow.holdSeats(userId, eventId, wanted);
					return true;
				} catch (SeatNoLongerAvailableException ex) {
					return false;
				}
			});
		}
		List<Boolean> results = race(tasks);

		boolean bigUserWon = results.get(0);
		long singleWinners = count(results.subList(1, results.size()));
		logger.info("5-seat user won: " + bigUserWon + ", single-seat winners: " + singleWinners);

		// all 8 seats end up HELD, never more than the capacity
		Event event = event(eventId);
		long held = event.getEventSeats().stream().filter(s -> s.getStatus() == SeatStatus.HELD).count();
		assertEquals(8, held);
		assertEquals(0, event.getEventSeats().stream().filter(s -> s.getStatus() == SeatStatus.SOLD).count());

		// if the 5-seat user won, singles got the other 3 seats; if not, singles got all 8
		assertEquals(bigUserWon ? 3 : 8, singleWinners);

		// all-or-nothing for the 5-seat user: one hold with exactly 5 seats, or no hold at all
		List<SeatHold> bigUserHolds = seatHoldRepository.toCollection().stream()
				.filter(h -> h.getEventId().equals(eventId) && h.getUserId().equals(bigUserId))
				.collect(Collectors.toList());
		if (bigUserWon) {
			assertEquals(1, bigUserHolds.size());
			assertEquals(5, bigUserHolds.get(0).getItems().size());
			for (int seatId = 1; seatId <= 5; seatId++)
				assertEquals(bigUserHolds.get(0).getHoldId(), event.findEventSeat(seatId).getHoldId());
		} else {
			assertTrue(bigUserHolds.isEmpty());
		}

		// no seat is held by two reservations: every hold owns distinct seats, holds are consistent with seats
		Set<Integer> seatsInActiveHolds = new HashSet<>();
		for (SeatHold hold : seatHoldRepository.toCollection()) {
			if (!hold.getEventId().equals(eventId) || hold.getStatus() != HoldStatus.ACTIVE)
				continue;
			hold.getItems().forEach(item -> assertTrue(seatsInActiveHolds.add(item.getEventSeatId()),
					"seat " + item.getEventSeatId() + " is held by two reservations"));
		}
		assertEquals(8, seatsInActiveHolds.size());
		for (EventSeat seat : event.getEventSeats())
			assertFalse(seat.getHoldId() == null);
	}

	/* ---------------- ticket cloning fraud: the same QR scanned at many gates at once ---------------- */

	/** true = VALID (entry allowed), false = ALREADY_USED. Any other result fails the test. */
	private Boolean scan(String qrToken, Integer eventId) {
		TicketScanResult result = workflow.validateTicket(qrToken, eventId).getResult();
		if (result != TicketScanResult.VALID && result != TicketScanResult.ALREADY_USED)
			throw new IllegalStateException("Unexpected scan result: " + result);
		return result == TicketScanResult.VALID;
	}

	/* One ticket forwarded to 20 people, scanned by 20 gates in the same moment: only ONE entry is allowed. */
	@Test
	public void twentyGatesScanningTheSameTicketOnlyOneIsValid() throws Exception {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer orderId = workflow.createOrder(workflow.holdSeats(buyer.getUserId(), eventId, seats(1)));
		Integer ticketId = workflow.confirmPayment(orderId).get(0);
		String qrToken = ticketRepository.getById(ticketId).getQrToken();

		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (int gate = 0; gate < 20; gate++)
			tasks.add(() -> scan(qrToken, eventId));
		List<Boolean> results = race(tasks);

		assertEquals(1, count(results));                                        // exactly one VALID
		assertEquals(19, results.stream().filter(r -> !r).count());             // 19 ALREADY_USED
		assertEquals(TicketStatus.USED, ticketRepository.getById(ticketId).getStatus());
		assertEquals(1, workflow.getEventSalesReport(eventId).getScannedTickets());
	}

	/* 5 different tickets, each one scanned by 20 gates at once: exactly 5 entries, one per ticket. */
	@Test
	public void everyTicketAdmitsExactlyOnePersonEvenWhenManyTicketsAreScannedAtOnce() throws Exception {
		Integer eventId = newPublishedEvent(1, 8);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer orderId = workflow.createOrder(workflow.holdSeats(buyer.getUserId(), eventId, seats(1, 2, 3, 4, 5)));
		List<Integer> ticketIds = workflow.confirmPayment(orderId);

		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (Integer ticketId : ticketIds) {
			String qrToken = ticketRepository.getById(ticketId).getQrToken();
			for (int gate = 0; gate < 20; gate++)
				tasks.add(() -> scan(qrToken, eventId));
		}
		List<Boolean> results = race(tasks);

		for (int t = 0; t < ticketIds.size(); t++)
			assertEquals(1, count(results.subList(t * 20, (t + 1) * 20)), "ticket #" + t);
		EventSalesView report = workflow.getEventSalesReport(eventId);
		assertEquals(5, report.getScannedTickets());
		assertEquals(0, report.getNotAttended());
	}
}
