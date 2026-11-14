package org.elide.events.domain.services;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.elide.events.domain.sales.HoldStatus;
import org.elide.events.domain.sales.SeatHold;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ISeatHoldRepository extends IEntityRepository<SeatHold> {

	List<SeatHold> findByEventIdAndStatus(Integer eventId, HoldStatus status);

	@Query("select h from SeatHold h where h.eventId = :eventId and h.status = :status and h.expiresAt < :now order by h.holdId")
	List<SeatHold> findByEventIdAndStatusExpiredBefore(@Param("eventId") Integer eventId,
			@Param("status") HoldStatus status, @Param("now") LocalDateTime now);

	/** ACTIVE holds of the event whose expiry time is before {@code now}. */
	default List<SeatHold> findExpiredActiveByEventId(Integer eventId, LocalDateTime now) {
		return findByEventIdAndStatusExpiredBefore(eventId, HoldStatus.ACTIVE, now);
	}

	@Query("select distinct h.eventId from SeatHold h where h.status = :status and h.expiresAt < :now")
	List<Integer> findEventIdsWithHoldsExpiredBefore(@Param("status") HoldStatus status, @Param("now") LocalDateTime now);

	/** Distinct events that have ACTIVE holds expired before {@code now} (input of the periodic expiry job). */
	default List<Integer> findEventIdsWithExpiredActiveHolds(LocalDateTime now) {
		return findEventIdsWithHoldsExpiredBefore(HoldStatus.ACTIVE, now);
	}

	/** Scalar lookup (nothing is loaded): the event of a hold, or null. Used to lock the event before touching the hold. */
	@Query("select h.eventId from SeatHold h where h.holdId = :holdId")
	Integer findEventIdByHoldId(@Param("holdId") Integer holdId);

	@Query("select count(i) from SeatHold h join h.items i "
			+ "where h.userId = :userId and h.eventId = :eventId and h.status in (:statuses)")
	long countSeatsByUserAndEventAndStatusIn(@Param("userId") Integer userId, @Param("eventId") Integer eventId,
			@Param("statuses") Collection<HoldStatus> statuses);

	/** Seats of the user for one event: currently held (ACTIVE holds) + bought (CONVERTED holds). */
	default long countSeatsOwnedByUser(Integer userId, Integer eventId) {
		return countSeatsByUserAndEventAndStatusIn(userId, eventId, Arrays.asList(HoldStatus.ACTIVE, HoldStatus.CONVERTED));
	}
}
