package org.elide.events.domain.services;

import java.util.List;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.event.EventStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IEventRepository extends IEntityRepository<Event> {

	// Queryable named operations (derived from the method name)
	List<Event> findByStatus(EventStatus status);

	List<Event> findByOrganizerId(Integer organizerId);

	// Queryable annotated operation: custom filter
	@Query("select e from Event e where lower(e.title) like lower(concat('%', :text, '%'))")
	List<Event> findByTitleContaining(@Param("text") String text);

	/**
	 * Locks the Event row (SELECT ... FOR UPDATE). Plain single-table native query: PostgreSQL cannot lock the nullable
	 * side of the outer joins that loading the Event with its hall would add. Needs a running transaction.
	 */
	@Query(value = "select eventId from Event where eventId = :id for update", nativeQuery = true)
	Integer lockRowById(@Param("id") Integer id);

	/**
	 * The Event row is the lock of the whole aggregate (event + its seats): every operation that changes seat states
	 * locks it first, so concurrent transactions on the same event run one after another and no seat is sold or held
	 * twice. The aggregate is loaded after the lock was obtained, so it has the committed state.
	 */
	default Event getByIdForUpdate(Integer id) {
		if (id == null || lockRowById(id) == null)
			return null;
		return getByIdOrNull(id);
	}
}
