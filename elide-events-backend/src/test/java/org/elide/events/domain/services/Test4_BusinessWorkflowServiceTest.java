package org.elide.events.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.event.EventSalesView;
import org.elide.events.domain.event.EventStatus;
import org.elide.events.domain.event.SeatStatus;
import org.elide.events.domain.event.TicketType;
import org.elide.events.domain.identity.AccountStatus;
import org.elide.events.domain.identity.UserAccount;
import org.elide.events.domain.identity.UserRole;
import org.elide.events.domain.sales.HoldStatus;
import org.elide.events.domain.sales.Order;
import org.elide.events.domain.sales.OrderStatus;
import org.elide.events.domain.sales.TicketScanResult;
import org.elide.events.domain.sales.TicketValidationView;
import org.junit.jupiter.api.Test;

/** Business Workflow (use-case) Services: create/publish event, hold seats, order, pay, scan. */
public class Test4_BusinessWorkflowServiceTest extends AbstractDomainTest {

	/* ---------------- event creation / publication / edit rules ---------------- */

	@Test
	public void organizerCreatesAndPublishesEvent() {
		Integer eventId = newDraftEvent(2, 5);
		assertEquals(EventStatus.DRAFT, event(eventId).getStatus());

		workflow.publishEvent(eventId);

		assertEquals(EventStatus.PUBLISHED, event(eventId).getStatus());
		assertTrue(event(eventId).getPublishedAt() != null);
	}

	@Test
	public void participantCannotCreateEvent() {
		UserAccount participant = newUser(UserRole.PARTICIPANT);
		Integer hallId = newHall(1, 2).getHallId();
		assertThrows(BusinessRuleViolationException.class, () -> workflow.createDraftEvent(
				participant.getUserId(), hallId, "Concert", inDays(5), inDays(5).plusHours(2), BigDecimal.TEN));
	}

	@Test
	public void invalidEventIsRejectedByValidationService() {
		UserAccount organizer = newUser(UserRole.ORGANIZER);
		Integer hallId = newHall(1, 2).getHallId();
		assertThrows(DomainValidationException.class, () -> workflow.createDraftEvent(
				organizer.getUserId(), hallId, "Concert", inDays(-5), inDays(-5).plusHours(2), BigDecimal.TEN));
	}

	@Test
	public void publishedEventCannotBePublishedTwice() {
		Integer eventId = newPublishedEvent(1, 2);
		assertThrows(BusinessRuleViolationException.class, () -> workflow.publishEvent(eventId));
	}

	@Test
	public void draftAndUnsoldPublishedEventCanBeRescheduled() {
		Integer draftId = newDraftEvent(1, 3);
		workflow.rescheduleEvent(draftId, inDays(40), inDays(40).plusHours(2));
		assertEquals(inDays(40).toLocalDate(), event(draftId).getStartAt().toLocalDate());

		Integer publishedId = newPublishedEvent(1, 3);
		workflow.rescheduleEvent(publishedId, inDays(41), inDays(41).plusHours(2));
		assertEquals(inDays(41).toLocalDate(), event(publishedId).getStartAt().toLocalDate());
	}

	@Test
	public void publishedEventWithSoldSeatsCannotBeRescheduled() {
		Integer eventId = newPublishedEvent(1, 3);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer orderId = workflow.createOrder(workflow.holdSeats(buyer.getUserId(), eventId, seats(1)));
		workflow.confirmPayment(orderId);

		assertThrows(BusinessRuleViolationException.class,
				() -> workflow.rescheduleEvent(eventId, inDays(50), inDays(50).plusHours(2)));
	}

	@Test
	public void rescheduleWithInvalidDatesKeepsOldSchedule() {
		Integer eventId = newDraftEvent(1, 3);
		LocalDateTime oldStart = event(eventId).getStartAt();
		assertThrows(DomainValidationException.class,
				() -> workflow.rescheduleEvent(eventId, inDays(50), inDays(49)));
		assertEquals(oldStart, event(eventId).getStartAt());
	}

	/* ---------------- seat hold ---------------- */

	@Test
	public void seatsOfADraftEventCannotBeHeld() {
		Integer eventId = newDraftEvent(1, 3);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		assertThrows(BusinessRuleViolationException.class,
				() -> workflow.holdSeats(buyer.getUserId(), eventId, seats(1)));
	}

	@Test
	public void holdSetsSeatsToHeldForTenMinutes() {
		Integer eventId = newPublishedEvent(3, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);

		Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(1, 2, 3));

		Event event = event(eventId);
		assertEquals(SeatStatus.HELD, event.findEventSeat(1).getStatus());
		assertEquals(SeatStatus.HELD, event.findEventSeat(3).getStatus());
		assertEquals(SeatStatus.AVAILABLE, event.findEventSeat(4).getStatus());
		assertEquals(HoldStatus.ACTIVE, seatHoldRepository.getById(holdId).getStatus());
		assertEquals(10, java.time.Duration.between(seatHoldRepository.getById(holdId).getCreatedAt(),
				seatHoldRepository.getById(holdId).getExpiresAt()).toMinutes());
	}

	@Test
	public void secondUserCannotTakeAHeldSeatAndNothingIsChanged() {
		Integer eventId = newPublishedEvent(3, 4);
		UserAccount userA = newUser(UserRole.PARTICIPANT);
		UserAccount userB = newUser(UserRole.PARTICIPANT);
		workflow.holdSeats(userA.getUserId(), eventId, seats(1));

		SeatNoLongerAvailableException ex = assertThrows(SeatNoLongerAvailableException.class,
				() -> workflow.holdSeats(userB.getUserId(), eventId, seats(2, 1)));

		assertTrue(ex.getMessage().contains("SEAT_NO_LONGER_AVAILABLE"));
		// all-or-nothing: seat 2 was NOT held by the failed request
		assertEquals(SeatStatus.AVAILABLE, event(eventId).findEventSeat(2).getStatus());
	}

	@Test
	public void twoUsersRacingForTheSameSeatOnlyOneWins() throws Exception {
		Integer eventId = newPublishedEvent(2, 5);
		UserAccount userA = newUser(UserRole.PARTICIPANT);
		UserAccount userB = newUser(UserRole.PARTICIPANT);

		CountDownLatch start = new CountDownLatch(1);
		Callable<Boolean> a = () -> tryHold(start, userA.getUserId(), eventId);
		Callable<Boolean> b = () -> tryHold(start, userB.getUserId(), eventId);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		Future<Boolean> fa = pool.submit(a);
		Future<Boolean> fb = pool.submit(b);
		start.countDown();
		boolean okA = fa.get();
		boolean okB = fb.get();
		pool.shutdown();

		assertNotEquals(okA, okB, "exactly one user must obtain the seat");
		assertEquals(SeatStatus.HELD, event(eventId).findEventSeat(1).getStatus());
	}

	private Boolean tryHold(CountDownLatch start, Integer userId, Integer eventId) throws InterruptedException {
		start.await();
		try {
			workflow.holdSeats(userId, eventId, seats(1));
			return true;
		} catch (SeatNoLongerAvailableException ex) {
			return false;
		}
	}

	/* ---------------- expiry ---------------- */

	@Test
	public void expiredHoldReleasesSeatsAndTheOrder() {
		Integer eventId = newPublishedEvent(2, 5);
		UserAccount userA = newUser(UserRole.PARTICIPANT);
		UserAccount userB = newUser(UserRole.PARTICIPANT);
		Integer holdId = workflow.holdSeats(userA.getUserId(), eventId, seats(1, 2));
		Integer orderId = workflow.createOrder(holdId);

		int expired = workflow.expireHolds(LocalDateTime.now().plusMinutes(11));

		assertTrue(expired >= 1);
		assertEquals(HoldStatus.EXPIRED, seatHoldRepository.getById(holdId).getStatus());
		assertEquals(OrderStatus.EXPIRED, orderRepository.getById(orderId).getStatus());
		assertEquals(SeatStatus.AVAILABLE, event(eventId).findEventSeat(1).getStatus());
		// the seat can be taken again by someone else
		workflow.holdSeats(userB.getUserId(), eventId, seats(1));
		assertEquals(SeatStatus.HELD, event(eventId).findEventSeat(1).getStatus());
	}

	@Test
	public void holdStillValidBeforeTenMinutesIsNotExpired() {
		Integer eventId = newPublishedEvent(1, 3);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(1));

		workflow.expireHolds(LocalDateTime.now().plusMinutes(5));

		assertEquals(HoldStatus.ACTIVE, seatHoldRepository.getById(holdId).getStatus());
		assertEquals(SeatStatus.HELD, event(eventId).findEventSeat(1).getStatus());
	}

	/* ---------------- order + payment ---------------- */

	@Test
	public void fullPurchaseFlowIssuesTicketsAndFeedsTheReport() {
		Integer eventId = newPublishedEvent(3, 4); // 12 seats x 100.00
		UserAccount buyer = newUser(UserRole.PARTICIPANT);

		Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(1, 2, 3));
		Integer orderId = workflow.createOrder(holdId);

		Order order = orderRepository.getById(orderId);
		assertEquals(OrderStatus.PENDING, order.getStatus());
		assertEquals(new BigDecimal("300.00"), order.getTotal());

		List<Integer> ticketIds = workflow.confirmPayment(orderId);

		assertEquals(3, ticketIds.size());
		assertEquals(OrderStatus.PAID, orderRepository.getById(orderId).getStatus());
		assertEquals(HoldStatus.CONVERTED, seatHoldRepository.getById(holdId).getStatus());
		assertEquals(SeatStatus.SOLD, event(eventId).findEventSeat(2).getStatus());
		assertTrue(ticketRepository.getById(ticketIds.get(0)).getQrToken().length() >= 32);

		EventSalesView report = workflow.getEventSalesReport(eventId);
		assertEquals(12, report.getCapacity());
		assertEquals(3, report.getSoldSeats());
		assertEquals(9, report.getAvailableSeats());
		assertEquals(new BigDecimal("300.00"), report.getGrossRevenue());
		assertEquals(new BigDecimal("25.00"), report.getOccupancyRate());
		assertEquals(0, report.getScannedTickets());
		assertEquals(3, report.getNotAttended());
	}

	@Test
	public void failedPaymentReleasesSeats() {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(1, 2));
		Integer orderId = workflow.createOrder(holdId);

		workflow.failPayment(orderId);

		assertEquals(OrderStatus.FAILED, orderRepository.getById(orderId).getStatus());
		assertEquals(HoldStatus.CANCELLED, seatHoldRepository.getById(holdId).getStatus());
		assertEquals(SeatStatus.AVAILABLE, event(eventId).findEventSeat(1).getStatus());
		assertEquals(SeatStatus.AVAILABLE, event(eventId).findEventSeat(2).getStatus());
	}

	@Test
	public void cannotCreateOrderFromExpiredHold() {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(1));
		workflow.expireHolds(LocalDateTime.now().plusMinutes(11));

		assertThrows(BusinessRuleViolationException.class, () -> workflow.createOrder(holdId));
	}

	@Test
	public void paymentAfterExpiryDoesNotIssueTickets() {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer orderId = workflow.createOrder(workflow.holdSeats(buyer.getUserId(), eventId, seats(1)));
		workflow.expireHolds(LocalDateTime.now().plusMinutes(11));

		assertThrows(BusinessRuleViolationException.class, () -> workflow.confirmPayment(orderId));
		assertEquals(0, ticketRepository.findByEventId(eventId).size());
	}

	@Test
	public void orderCannotBePaidTwice() {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer orderId = workflow.createOrder(workflow.holdSeats(buyer.getUserId(), eventId, seats(1)));
		workflow.confirmPayment(orderId);

		assertThrows(BusinessRuleViolationException.class, () -> workflow.confirmPayment(orderId));
		assertEquals(1, ticketRepository.findByEventId(eventId).size());
	}

	/* ---------------- ticket validation ---------------- */

	@Test
	public void qrValidationCoversAllScanResults() {
		Integer eventId = newPublishedEvent(1, 4);
		Integer otherEventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer orderId = workflow.createOrder(workflow.holdSeats(buyer.getUserId(), eventId, seats(1)));
		Integer ticketId = workflow.confirmPayment(orderId).get(0);
		String qr = ticketRepository.getById(ticketId).getQrToken();

		assertEquals(TicketScanResult.INVALID, workflow.validateTicket("unknown-token", eventId).getResult());
		assertEquals(TicketScanResult.WRONG_EVENT, workflow.validateTicket(qr, otherEventId).getResult());

		TicketValidationView first = workflow.validateTicket(qr, eventId);
		assertEquals(TicketScanResult.VALID, first.getResult());
		assertEquals("A1", first.getSeatLabel());
		assertEquals("STANDARD", first.getTicketTypeName());

		assertEquals(TicketScanResult.ALREADY_USED, workflow.validateTicket(qr, eventId).getResult());

		EventSalesView report = workflow.getEventSalesReport(eventId);
		assertEquals(1, report.getScannedTickets());
		assertEquals(0, report.getNotAttended());
	}

	@Test
	public void cancelledTicketIsRejectedAtTheEntrance() {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer orderId = workflow.createOrder(workflow.holdSeats(buyer.getUserId(), eventId, seats(1)));
		Integer ticketId = workflow.confirmPayment(orderId).get(0);
		ticketRepository.getById(ticketId).setStatus(org.elide.events.domain.sales.TicketStatus.CANCELLED);

		String qr = ticketRepository.getById(ticketId).getQrToken();
		assertEquals(TicketScanResult.CANCELLED, workflow.validateTicket(qr, eventId).getResult());
	}

	/* ---------------- organizer approval ---------------- */

	private Integer draftEventOf(UserAccount organizer) {
		Integer hallId = newHall(1, 4).getHallId();
		return workflow.createDraftEvent(organizer.getUserId(), hallId, "Concert Organizer",
				inDays(30), inDays(30).plusHours(3), new BigDecimal("100.00"));
	}

	@Test
	public void pendingOrganizerCanPrepareADraftButCannotPublish() {
		UserAccount organizer = newUser(UserRole.ORGANIZER);
		organizer.setStatus(AccountStatus.PENDING);

		Integer eventId = draftEventOf(organizer);

		assertThrows(BusinessRuleViolationException.class, () -> workflow.publishEvent(eventId));
		assertEquals(EventStatus.DRAFT, event(eventId).getStatus());
	}

	@Test
	public void organizerCanPublishAfterAdministratorApproval() {
		UserAccount organizer = newUser(UserRole.ORGANIZER);
		organizer.setStatus(AccountStatus.PENDING);
		UserAccount admin = newUser(UserRole.ADMINISTRATOR);
		Integer eventId = draftEventOf(organizer);

		workflow.approveOrganizer(admin.getUserId(), organizer.getUserId());
		workflow.publishEvent(eventId);

		assertEquals(AccountStatus.ACTIVE, organizer.getStatus());
		assertEquals(EventStatus.PUBLISHED, event(eventId).getStatus());
	}

	@Test
	public void onlyAnAdministratorCanApproveAPendingOrganizer() {
		UserAccount organizer = newUser(UserRole.ORGANIZER);
		organizer.setStatus(AccountStatus.PENDING);
		UserAccount otherOrganizer = newUser(UserRole.ORGANIZER);
		UserAccount participant = newUser(UserRole.PARTICIPANT);
		UserAccount admin = newUser(UserRole.ADMINISTRATOR);

		assertThrows(BusinessRuleViolationException.class,
				() -> workflow.approveOrganizer(otherOrganizer.getUserId(), organizer.getUserId()));
		assertThrows(BusinessRuleViolationException.class,
				() -> workflow.approveOrganizer(participant.getUserId(), organizer.getUserId()));
		assertEquals(AccountStatus.PENDING, organizer.getStatus());

		workflow.approveOrganizer(admin.getUserId(), organizer.getUserId());
		// already approved: it is not PENDING anymore
		assertThrows(BusinessRuleViolationException.class,
				() -> workflow.approveOrganizer(admin.getUserId(), organizer.getUserId()));
	}

	@Test
	public void suspendedOrganizerCannotCreateNorPublish() {
		UserAccount organizer = newUser(UserRole.ORGANIZER);
		Integer eventId = draftEventOf(organizer);
		organizer.setStatus(AccountStatus.SUSPENDED);

		assertThrows(BusinessRuleViolationException.class, () -> workflow.publishEvent(eventId));
		assertThrows(BusinessRuleViolationException.class, () -> draftEventOf(organizer));
	}

	/* ---------------- ticket category capacity reduction ---------------- */

	/** 3 rows x 4 seats: row A = VIP (seats 1..4), rows B and C = STANDARD (seats 5..12). */
	private Integer publishedEventWithVipRow() {
		Integer eventId = newDraftEvent(3, 4);
		TicketType vip = event(eventId).addTicketType("VIP", new BigDecimal("250.00"));
		event(eventId).assignTicketTypeToRow("A", vip);
		workflow.publishEvent(eventId);
		return eventId;
	}

	@Test
	public void categoryCapacityCannotGoBelowSoldPlusHeldTickets() {
		Integer eventId = publishedEventWithVipRow();
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		UserAccount otherBuyer = newUser(UserRole.PARTICIPANT);
		workflow.confirmPayment(workflow.createOrder(workflow.holdSeats(buyer.getUserId(), eventId, seats(1)))); // A1 SOLD
		workflow.holdSeats(otherBuyer.getUserId(), eventId, seats(2));                                           // A2 HELD

		// 1 sold + 1 held = 2 committed VIP seats
		assertThrows(BusinessRuleViolationException.class, () -> workflow.reduceCategoryCapacity(eventId, "VIP", 1));
		assertEquals(4, event(eventId).getEventSeats().stream()
				.filter(s -> s.getTicketType().getName().equals("VIP") && s.getStatus() != SeatStatus.BLOCKED).count());

		// down to exactly the committed number is allowed: the 2 free VIP seats are blocked
		Integer blocked = workflow.reduceCategoryCapacity(eventId, "VIP", 2);

		assertEquals(2, blocked);
		assertEquals(SeatStatus.SOLD, event(eventId).findEventSeat(1).getStatus());
		assertEquals(SeatStatus.HELD, event(eventId).findEventSeat(2).getStatus());
		assertEquals(SeatStatus.BLOCKED, event(eventId).findEventSeat(3).getStatus());
		assertEquals(SeatStatus.BLOCKED, event(eventId).findEventSeat(4).getStatus());
		assertEquals(10, event(eventId).getCapacity()); // 12 seats - 2 blocked
	}

	@Test
	public void reducedSeatsCanNoLongerBeHeldAndOtherCategoriesAreUntouched() {
		Integer eventId = publishedEventWithVipRow();
		UserAccount buyer = newUser(UserRole.PARTICIPANT);

		workflow.reduceCategoryCapacity(eventId, "VIP", 1); // A2, A3, A4 blocked (highest ids first)

		assertThrows(SeatNoLongerAvailableException.class,
				() -> workflow.holdSeats(buyer.getUserId(), eventId, seats(4)));
		assertEquals(SeatStatus.AVAILABLE, event(eventId).findEventSeat(1).getStatus());
		assertEquals(8, event(eventId).getEventSeats().stream()
				.filter(s -> s.getTicketType().getName().equals("STANDARD") && s.getStatus() == SeatStatus.AVAILABLE).count());
		workflow.holdSeats(buyer.getUserId(), eventId, seats(5)); // a STANDARD seat is still sellable
	}

	@Test
	public void capacityReductionOnlyReducesAndOnlyKnownCategories() {
		Integer eventId = publishedEventWithVipRow();

		assertThrows(BusinessRuleViolationException.class, () -> workflow.reduceCategoryCapacity(eventId, "VIP", 5));
		assertThrows(BusinessRuleViolationException.class, () -> workflow.reduceCategoryCapacity(eventId, "VIP", -1));
		assertThrows(BusinessRuleViolationException.class, () -> workflow.reduceCategoryCapacity(eventId, "BALCONY", 1));
		assertEquals(12, event(eventId).getCapacity());
	}

	/* ---------------- payment authorization: no charge after the hold expired ---------------- */

	/** Simulates that the 10 minutes of the hold have passed, without waiting. */
	private void expireHoldTimeOf(Integer holdId) {
		seatHoldRepository.getById(holdId).setExpiresAt(LocalDateTime.now().minusMinutes(1));
	}

	@Test
	public void paymentIsRejectedBeforeChargingWhenTheHoldHasExpired() {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(1));
		Integer orderId = workflow.createOrder(holdId);
		expireHoldTimeOf(holdId); // the buyer was too slow

		PaymentRejectedException ex = assertThrows(PaymentRejectedException.class, () -> workflow.startPayment(orderId));

		assertTrue(ex.getMessage().contains("PAYMENT_REJECTED"));
		assertEquals(OrderStatus.EXPIRED, orderRepository.getById(orderId).getStatus());
		assertEquals(HoldStatus.EXPIRED, seatHoldRepository.getById(holdId).getStatus());
		assertEquals(SeatStatus.AVAILABLE, event(eventId).findEventSeat(1).getStatus());
		assertEquals(0, ticketRepository.findByEventId(eventId).size());
		// asking again after the rejection is rejected as well
		assertThrows(PaymentRejectedException.class, () -> workflow.startPayment(orderId));
	}

	@Test
	public void paymentStartedInTimeIsProcessedAndConfirmed() {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer orderId = workflow.createOrder(workflow.holdSeats(buyer.getUserId(), eventId, seats(1)));

		workflow.startPayment(orderId);
		assertEquals(OrderStatus.PAYMENT_PROCESSING, orderRepository.getById(orderId).getStatus());

		assertEquals(1, workflow.confirmPayment(orderId).size());
		assertEquals(OrderStatus.PAID, orderRepository.getById(orderId).getStatus());
	}

	@Test
	public void seatsOfAStartedPaymentAreProtectedWhileTheCardIsBeingCharged() {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		UserAccount other = newUser(UserRole.PARTICIPANT);
		Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(1));
		Integer orderId = workflow.createOrder(holdId);
		workflow.startPayment(orderId); // card charging starts in time
		expireHoldTimeOf(holdId);       // the result arrives a little after the 10 minutes

		workflow.expireHolds(LocalDateTime.now());

		assertEquals(HoldStatus.ACTIVE, seatHoldRepository.getById(holdId).getStatus());
		assertThrows(SeatNoLongerAvailableException.class,
				() -> workflow.holdSeats(other.getUserId(), eventId, seats(1)));
		assertEquals(1, workflow.confirmPayment(orderId).size()); // honoured, the buyer paid in time
	}

	@Test
	public void paymentStuckBeyondTheGracePeriodIsExpired() {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(1));
		Integer orderId = workflow.createOrder(holdId);
		workflow.startPayment(orderId);

		workflow.expireHolds(LocalDateTime.now().plusMinutes(10 + EventTicketingBusinessWorkflowServiceImpl.PAYMENT_GRACE_MINUTES + 1));

		assertEquals(HoldStatus.EXPIRED, seatHoldRepository.getById(holdId).getStatus());
		assertEquals(OrderStatus.EXPIRED, orderRepository.getById(orderId).getStatus());
		assertEquals(SeatStatus.AVAILABLE, event(eventId).findEventSeat(1).getStatus());
		// a result that arrives this late issues no ticket (the payment layer has to refund it)
		assertThrows(BusinessRuleViolationException.class, () -> workflow.confirmPayment(orderId));
		assertEquals(0, ticketRepository.findByEventId(eventId).size());
	}

	@Test
	public void paymentCannotBeStartedTwiceAndAFailedStartedPaymentReleasesSeats() {
		Integer eventId = newPublishedEvent(1, 4);
		UserAccount buyer = newUser(UserRole.PARTICIPANT);
		Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(1));
		Integer orderId = workflow.createOrder(holdId);
		workflow.startPayment(orderId);

		assertThrows(BusinessRuleViolationException.class, () -> workflow.startPayment(orderId));

		workflow.failPayment(orderId); // the card was declined
		assertEquals(OrderStatus.FAILED, orderRepository.getById(orderId).getStatus());
		assertEquals(HoldStatus.CANCELLED, seatHoldRepository.getById(holdId).getStatus());
		assertEquals(SeatStatus.AVAILABLE, event(eventId).findEventSeat(1).getStatus());
	}
}
