package org.elide.events.domain.services;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.event.SeatStatus;
import org.springframework.stereotype.Service;

/* Naming template: Action.ing Entity-name Service-type */
@Service
public class SummarizingEventDomainServiceImpl implements ISummarizingEventDomainService {

	@Override
	public Event countingSeats(Event event) {
		event.setAvailableCount(count(event, SeatStatus.AVAILABLE));
		event.setHeldCount(count(event, SeatStatus.HELD));
		event.setSoldCount(count(event, SeatStatus.SOLD));
		event.setBlockedCount(count(event, SeatStatus.BLOCKED));
		return event;
	}

	@Override
	public Event computingOccupancy(Event event) {
		countingSeats(event);
		int capacity = event.getCapacity();
		BigDecimal rate = (capacity == 0) ? BigDecimal.ZERO.setScale(2)
				: BigDecimal.valueOf(event.getSoldCount())
					.multiply(BigDecimal.valueOf(100))
					.divide(BigDecimal.valueOf(capacity), 2, RoundingMode.HALF_UP);
		event.setOccupancyRate(rate);
		return event;
	}

	private int count(Event event, SeatStatus status) {
		return (int) event.getEventSeats().stream().filter(s -> s.getStatus() == status).count();
	}
}
