package org.elide.events.infrastructure;

import java.util.function.Supplier;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ITransactionManager over the Spring transaction infrastructure (JpaTransactionManager configured by Spring Boot):
 * the workflow steps run in a TransactionTemplate (propagation REQUIRED: a step called inside another transaction joins it;
 * COMMIT on normal return, ROLLBACK when a RuntimeException is thrown). The Spring Data repositories take part in
 * the same transaction (same EntityManager, same JDBC connection).
 */
@Component
public class SpringTransactionManager implements ITransactionManager {

	private final TransactionTemplate transactionTemplate;

	/** Shared EntityManager proxy: it delegates to the EntityManager of the running transaction. */
	@PersistenceContext
	private EntityManager entityManager;

	public SpringTransactionManager(PlatformTransactionManager platformTransactionManager) {
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	@Override
	public <T> T inTransaction(Supplier<T> work) {
		return transactionTemplate.execute(status -> work.get());
	}

	@Override
	public void runInTransaction(Runnable work) {
		inTransaction(() -> {
			work.run();
			return null;
		});
	}

	@Override
	public boolean isTransactionActive() {
		return TransactionSynchronizationManager.isActualTransactionActive();
	}

	@Override
	public EntityManager currentEntityManager() {
		if (!isTransactionActive())
			throw new IllegalStateException("No transaction is running on this thread!");
		return entityManager;
	}
}
