package org.elide.events.domain.audit;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

/**
 * JPA entity listener (audit rule): logs every insert / update / delete of the audited aggregates.
 * Registered with {@code @EntityListeners(DomainAuditListener.class)} on Event, Order, SeatHold, Ticket, UserAccount.
 * The callbacks run inside the transaction of the operation that changes the entity, so the audit line is written
 * only for operations that are really flushed.
 */
public class DomainAuditListener {
	private static final Logger logger = Logger.getLogger(DomainAuditListener.class.getName());

	public enum EntityOperation {
		ADDED, UPDATED, DELETED
	}

	@PrePersist
	public void onPrePersist(Object entity) {
		audit(entity, EntityOperation.ADDED);
	}

	@PreUpdate
	public void onPreUpdate(Object entity) {
		audit(entity, EntityOperation.UPDATED);
	}

	@PreRemove
	public void onPreRemove(Object entity) {
		audit(entity, EntityOperation.DELETED);
	}

	private void audit(Object entity, EntityOperation operation) {
		// updates are very frequent (every seat/ticket state change): FINE; creations and deletions: INFO
		Level level = (operation == EntityOperation.UPDATED) ? Level.FINE : Level.INFO;
		if (logger.isLoggable(level))
			logger.log(level, ">>> JPA DomainAuditListener: " + entity + " > " + operation);
	}
}
