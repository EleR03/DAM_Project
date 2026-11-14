package org.elide.events.domain.services;

import java.util.List;

import javax.persistence.LockModeType;

import org.elide.events.domain.sales.Ticket;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ITicketRepository extends IEntityRepository<Ticket> {
	Ticket findByQrToken(String qrToken);

	/**
	 * Ticket with this QR token, row locked (SELECT ... FOR UPDATE) until the end of the running transaction: concurrent
	 * scans of the same ticket are serialized by the database, so only the first one can turn it into USED.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Ticket t where t.qrToken = :qrToken")
	Ticket findByQrTokenForUpdate(@Param("qrToken") String qrToken);

	List<Ticket> findByEventId(Integer eventId);

	List<Ticket> findByOrderId(Integer orderId);
}
