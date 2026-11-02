package org.elide.events.domain.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.elide.events.domain.event.EventSalesView;
import org.elide.events.domain.sales.TicketValidationView;

/*
 * Business Workflow (use-case) steps:
 * (0) Administrator approves the organizer account (PENDING -> ACTIVE) -> approveOrganizer
 * (1) Organizer creates a DRAFT event for a hall            -> createDraftEvent
 * (2) Organizer publishes it (only if the organizer account is approved) -> publishEvent
 *     reduce a ticket category capacity (not below sold + held)             -> reduceCategoryCapacity
 *     (before publication it can be edited, after it only if nothing is sold) -> rescheduleEvent
 *     CANCEL (operational): allowed any time before COMPLETED, even with sold tickets;
 *            compensation flow: holds released, tickets REFUNDED, refund requests returned -> cancelEvent
 *     DELETE (data removal): allowed only if no ticket was sold                -> deleteEvent
 * (3) Participant selects seats: 10-minute temporary hold   -> holdSeats
 * (4) Participant checks out: order created from the hold   -> createOrder
 * (5) Before charging the card, the payment layer asks permission  -> startPayment
 *     (hold expired -> PAYMENT_REJECTED, the card is NOT charged)
 *     Payment result (webhook): success -> SOLD + tickets   -> confirmPayment
 *                               failure -> seats released   -> failPayment
 * (6) Periodic job: expired holds free their seats          -> expireHolds
 * (7) Organizer scans QR at the entrance                    -> validateTicket
 * (8) Organizer sales report                                -> getEventSalesReport
 */
public interface IEventTicketingBusinessWorkflowService {

	/** PENDING organizer -> ACTIVE; only an ACTIVE administrator can do it. @return organizerId */
	Integer approveOrganizer(Integer adminId, Integer organizerId);

	/** Drafts can be created by ACTIVE or PENDING organizers; publishing needs an ACTIVE (approved) organizer. */
	Integer createDraftEvent(Integer organizerId, Integer hallId, String title,
			LocalDateTime startAt, LocalDateTime endAt, BigDecimal basePrice);

	Integer publishEvent(Integer eventId);

	/**
	 * Reduces the capacity of a ticket category (e.g. VIP) by blocking its surplus AVAILABLE seats.
	 * Rejected if newCapacity is below the sold + held tickets of the category, or above its current capacity.
	 * @return number of seats that were blocked
	 */
	Integer reduceCategoryCapacity(Integer eventId, String ticketTypeName, int newCapacity);

	Integer rescheduleEvent(Integer eventId, LocalDateTime newStartAt, LocalDateTime newEndAt);

	/**
	 * Operational cancellation (POST /events/{id}/cancel). Allowed in any status before COMPLETED, regardless of sold tickets.
	 * Idempotent: cancelling an already CANCELLED event changes nothing and returns an empty list.
	 * @return ids of the PAID orders that must be refunded (the payment layer will send the refund requests, e.g. to Stripe)
	 */
	List<Integer> cancelEvent(Integer eventId);

	/** Data removal (DELETE /events/{id}). Allowed only if no ticket was sold; otherwise the event must be cancelled instead. */
	Integer deleteEvent(Integer eventId);

	/**
	 * @return holdId; throws SeatNoLongerAvailableException (HTTP 409 later) if a seat is not AVAILABLE,
	 *         TicketLimitExceededException if the user would exceed the per-event ticket limit
	 */
	Integer holdSeats(Integer userId, Integer eventId, List<Integer> eventSeatIds);

	/** @return orderId */
	Integer createOrder(Integer holdId);

	/**
	 * Must be called by the payment layer BEFORE charging the card. If the 10-minute hold has expired the payment is
	 * rejected with PaymentRejectedException (seats released, order EXPIRED) and no money is taken.
	 * If accepted, the order becomes PAYMENT_PROCESSING and its seats stay held until the result arrives
	 * (at most PAYMENT_GRACE_MINUTES after the hold's expiry).
	 * @return orderId
	 */
	Integer startPayment(Integer orderId);

	/** @return ids of the issued tickets */
	List<Integer> confirmPayment(Integer orderId);

	Integer failPayment(Integer orderId);

	/** @return number of holds that were expired */
	int expireHolds(LocalDateTime now);

	TicketValidationView validateTicket(String qrToken, Integer eventId);

	EventSalesView getEventSalesReport(Integer eventId);
}
