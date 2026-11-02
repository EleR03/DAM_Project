package org.elide.events.domain.services;

import java.util.Collection;

/** Generic repository contract (in-memory implementation now; PostgreSQL/JPA in a later sprint). */
public interface IEntityRepository<T> {
	Integer getNextID();

	T getById(Integer id);

	Collection<T> toCollection();

	T add(T entity);

	boolean remove(T entity);

	int size();
}
