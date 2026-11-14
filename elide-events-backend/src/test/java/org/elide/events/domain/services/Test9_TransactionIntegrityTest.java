package org.elide.events.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.elide.events.domain.event.SeatStatus;
import org.elide.events.domain.identity.UserAccount;
import org.elide.events.domain.identity.UserRole;
import org.elide.events.domain.sales.HoldStatus;
import org.elide.events.domain.sales.OrderStatus;
import org.elide.events.domain.sales.SeatHold;
import org.elide.events.domain.sales.Ticket;
import org.elide.events.domain.sales.TicketScanResult;
import org.elide.events.domain.sales.TicketStatus;
import org.junit.jupiter.api.Test;

/**
 * Business services that change several entities are transactional: seats + holds + orders + order items + tickets
 * are committed together or rolled back together. Steps called inside an outer transaction join it, so the outer
 * code can roll back a whole sequence of workflow steps.
 */
public class Test9_TransactionIntegrityTest extends AbstractDomainTest {

    private static class Boom extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Boom() {
            super("simulated failure");
        }
    }

    private long activeHolds(Integer eventId) {
        return seatHoldRepository.findByEventIdAndStatus(eventId, HoldStatus.ACTIVE).size();
    }

    @Test
    public void failureInsideATransactionRollsBackEveryChangedEntity() {
        Integer eventId = newPublishedEvent(1, 4);
        UserAccount buyer = newUser(UserRole.PARTICIPANT);
        Integer seatId = seat(eventId, 1).getEventSeatId();

        assertThrows(Boom.class, () -> transactionManager.runInTransaction(() -> {
            SeatHold hold = new SeatHold(null, buyer.getUserId(), eventId, LocalDateTime.now(), LocalDateTime.now().plusMinutes(10));
            hold.addItem(seatId);
            seatHoldRepository.add(hold);
            eventRepository.getByIdOrNull(eventId).findEventSeat(seatId).hold(hold.getHoldId(), hold.getExpiresAt());
            throw new Boom(); // after the hold was inserted and the seat updated
        }));

        assertEquals(SeatStatus.AVAILABLE, seat(eventId, 1).getStatus());
        assertEquals(0, activeHolds(eventId));
    }

    @Test
    public void holdAndOrderStepsRolledBackTogetherWithTheCallerTransaction() {
        Integer eventId = newPublishedEvent(1, 4);
        UserAccount buyer = newUser(UserRole.PARTICIPANT);
        List<Integer> wanted = seats(eventId, 1, 2);

        assertThrows(Boom.class, () -> transactionManager.runInTransaction(() -> {
            Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, wanted);   // joins the transaction
            workflow.createOrder(holdId);                                            // sees the uncommitted hold
            throw new Boom();
        }));

        assertEquals(SeatStatus.AVAILABLE, seat(eventId, 1).getStatus());
        assertEquals(SeatStatus.AVAILABLE, seat(eventId, 2).getStatus());
        assertEquals(0, activeHolds(eventId));
        assertTrue(orderRepository.findByEventId(eventId).isEmpty());
        // nothing was left behind: the same seats can be held now
        assertNotNull(workflow.holdSeats(buyer.getUserId(), eventId, wanted));
    }

    @Test
    public void confirmPaymentChangesSeatsHoldOrderAndTicketsAsOneUnit() {
        Integer eventId = newPublishedEvent(1, 4);
        UserAccount buyer = newUser(UserRole.PARTICIPANT);
        Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(eventId, 1, 2));
        Integer orderId = workflow.createOrder(holdId);

        assertThrows(Boom.class, () -> transactionManager.runInTransaction(() -> {
            assertEquals(2, workflow.confirmPayment(orderId).size()); // seats SOLD, hold CONVERTED, order PAID, 2 tickets
            throw new Boom();                                         // ... and then everything is undone
        }));

        assertEquals(SeatStatus.HELD, seat(eventId, 1).getStatus());
        assertEquals(HoldStatus.ACTIVE, seatHoldRepository.getByIdOrNull(holdId).getStatus());
        assertEquals(OrderStatus.PENDING, orderRepository.getByIdOrNull(orderId).getStatus());
        assertTrue(ticketRepository.findByEventId(eventId).isEmpty());

        assertEquals(2, workflow.confirmPayment(orderId).size()); // the real confirmation still works
        assertEquals(SeatStatus.SOLD, seat(eventId, 1).getStatus());
    }

    @Test
    public void orderItemsAreCreatedFromTheSeatsOfTheHold() {
        Integer eventId = newPublishedEvent(1, 4);
        UserAccount buyer = newUser(UserRole.PARTICIPANT);
        List<Integer> wanted = seats(eventId, 2, 3);
        Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, wanted);

        Integer orderId = workflow.createOrder(holdId);

        SeatHold hold = seatHoldRepository.getByIdOrNull(holdId);
        assertEquals(wanted, hold.getItems().stream().map(i -> i.getEventSeatId()).collect(java.util.stream.Collectors.toList()));
        assertEquals(wanted, orderRepository.getByIdOrNull(orderId).getItems().stream().map(i -> i.getEventSeatId())
                .collect(java.util.stream.Collectors.toList()));
        for (Integer seatId : wanted)
            assertEquals(holdId, event(eventId).findEventSeat(seatId).getHoldId()); // every seat points to its hold
    }

    @Test
    public void orderIsRefusedWhenTheHeldSeatsNoLongerMatchTheHold() {
        Integer eventId = newPublishedEvent(1, 4);
        UserAccount buyer = newUser(UserRole.PARTICIPANT);
        Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(eventId, 1, 2));
        // the seat is released behind the back of the hold (inconsistent data)
        transactionManager.runInTransaction(() -> eventRepository.getByIdOrNull(eventId).getEventSeats().get(1).release());

        assertThrows(BusinessRuleViolationException.class, () -> workflow.createOrder(holdId));
        assertTrue(orderRepository.findByEventId(eventId).isEmpty());
    }

    @Test
    public void ticketValidationIsAtomicAndRollsBackWithTheCallerTransaction() {
        Integer eventId = newPublishedEvent(1, 4);
        UserAccount buyer = newUser(UserRole.PARTICIPANT);
        Integer orderId = workflow.createOrder(workflow.holdSeats(buyer.getUserId(), eventId, seats(eventId, 1)));
        Integer ticketId = workflow.confirmPayment(orderId).get(0);
        String qr = ticketRepository.getByIdOrNull(ticketId).getQrToken();

        assertThrows(Boom.class, () -> transactionManager.runInTransaction(() -> {
            assertEquals(TicketScanResult.VALID, workflow.validateTicket(qr, eventId).getResult());
            assertEquals(TicketScanResult.ALREADY_USED, workflow.validateTicket(qr, eventId).getResult()); // same transaction
            throw new Boom();
        }));
        assertEquals(TicketStatus.ISSUED, ticketRepository.getByIdOrNull(ticketId).getStatus()); // the entry was not stored

        assertEquals(TicketScanResult.VALID, workflow.validateTicket(qr, eventId).getResult());
        Ticket used = ticketRepository.getByIdOrNull(ticketId);
        assertEquals(TicketStatus.USED, used.getStatus());
        assertNotNull(used.getUsedAt());
        assertEquals(TicketScanResult.ALREADY_USED, workflow.validateTicket(qr, eventId).getResult());
    }

    @Test
    public void rejectedPaymentStillCommitsTheExpiryOfTheHold() {
        Integer eventId = newPublishedEvent(1, 4);
        UserAccount buyer = newUser(UserRole.PARTICIPANT);
        Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(eventId, 1));
        Integer orderId = workflow.createOrder(holdId);
        transactionManager.runInTransaction(() ->
                seatHoldRepository.getByIdOrNull(holdId).setExpiresAt(LocalDateTime.now().minusMinutes(1)));

        assertThrows(PaymentRejectedException.class, () -> workflow.startPayment(orderId));

        // the exception did not roll the release back: seat free, hold and order expired
        assertEquals(SeatStatus.AVAILABLE, seat(eventId, 1).getStatus());
        assertEquals(HoldStatus.EXPIRED, seatHoldRepository.getByIdOrNull(holdId).getStatus());
        assertEquals(OrderStatus.EXPIRED, orderRepository.getByIdOrNull(orderId).getStatus());
    }
}
