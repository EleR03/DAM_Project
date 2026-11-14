package org.elide.events.infrastructure;

import java.util.function.Supplier;

import javax.persistence.EntityManager;

/**
 * Programmatic (resource-local) transaction demarcation, built directly on JPA: no Spring Boot, no Spring Data,
 * no @Transactional proxies. The unit of work is bound to the calling thread: the repositories called inside
 * {@link #inTransaction(Supplier)} all use the same EntityManager (persistence context) and the same JDBC
 * transaction, so a business operation that changes several entities commits or rolls back as a whole.
 */
public interface ITransactionManager {

	/**
	 * Runs {@code work} in a transaction. If a transaction is already running on this thread, the work simply joins it
	 * (propagation REQUIRED). Otherwise a new EntityManager + transaction are created: COMMIT when the work returns
	 * normally, ROLLBACK when it throws (the exception is rethrown unchanged).
	 */
	<T> T inTransaction(Supplier<T> work);

	void runInTransaction(Runnable work);

	/** True if a transaction is running on the calling thread. */
	boolean isTransactionActive();

	/** The EntityManager of the transaction running on the calling thread; fails if there is none. */
	EntityManager currentEntityManager();
}
