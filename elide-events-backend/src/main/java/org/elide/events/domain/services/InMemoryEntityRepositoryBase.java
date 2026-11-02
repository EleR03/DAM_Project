package org.elide.events.domain.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Thread-safe in-memory repository base. */
public abstract class InMemoryEntityRepositoryBase<T> implements IEntityRepository<T> {
	private final Map<Integer, T> entities = new ConcurrentHashMap<>();
	private final AtomicInteger nextId = new AtomicInteger(0);

	protected abstract Integer idOf(T entity);

	protected abstract void assignId(T entity, Integer id);

	@Override
	public Integer getNextID() {
		return nextId.incrementAndGet();
	}

	@Override
	public T getById(Integer id) {
		return (id == null) ? null : entities.get(id);
	}

	@Override
	public Collection<T> toCollection() {
		return new ArrayList<>(entities.values());
	}

	@Override
	public T add(T entity) {
		if (idOf(entity) == null)
			assignId(entity, getNextID());
		entities.put(idOf(entity), entity);
		return entity;
	}

	@Override
	public boolean remove(T entity) {
		return entity != null && idOf(entity) != null && entities.remove(idOf(entity)) != null;
	}

	@Override
	public int size() {
		return entities.size();
	}
}
