package org.elide.events.domain.services;

import java.util.List;

import org.elide.events.domain.venue.Hall;

public interface IHallRepository extends IEntityRepository<Hall> {
	List<Hall> findByName(String name);

	/** Property path: Hall.venue.city */
	List<Hall> findByVenueCity(String city);
}
