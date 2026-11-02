package org.elide.events.domain.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.identity.UserAccount;
import org.elide.events.domain.identity.UserRole;
import org.elide.events.domain.venue.Hall;
import org.elide.events.spring.AppScanConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Shared Spring context + fixtures. Every test builds its own data (the repositories are shared singletons). */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AppScanConfig.class)
public abstract class AbstractDomainTest {

	@Autowired protected IUserAccountRepository userRepository;
	@Autowired protected IHallRepository hallRepository;
	@Autowired protected IEventRepository eventRepository;
	@Autowired protected ISeatHoldRepository seatHoldRepository;
	@Autowired protected IOrderRepository orderRepository;
	@Autowired protected ITicketRepository ticketRepository;

	@Autowired @Qualifier("EventEntityFactoryBase")
	protected IEventEntityFactory factory;

	@Autowired @Qualifier("EventTicketingBusinessWorkflowServiceFacade")
	protected IEventTicketingBusinessWorkflowService workflow;

	protected LocalDateTime inDays(int days) {
		return LocalDateTime.now().plusDays(days);
	}

	protected UserAccount newUser(UserRole role) {
		UserAccount user = factory.buildUser("user" + System.nanoTime() + "@elide.test", "Test", role.name(), role);
		return userRepository.add(user);
	}

	protected Hall newHall(int rows, int seatsPerRow) {
		Hall hall = factory.buildHall(factory.buildVenue("Palatul Culturii", "Iasi"), "Sala Mare", rows, seatsPerRow);
		return hallRepository.add(hall);
	}

	/** Draft event (persisted in the repository) with rows x seatsPerRow seats, base price 100.00. */
	protected Integer newDraftEvent(int rows, int seatsPerRow) {
		UserAccount organizer = newUser(UserRole.ORGANIZER);
		Hall hall = newHall(rows, seatsPerRow);
		return workflow.createDraftEvent(organizer.getUserId(), hall.getHallId(), "Concert Test",
				inDays(30), inDays(30).plusHours(3), new BigDecimal("100.00"));
	}

	protected Integer newPublishedEvent(int rows, int seatsPerRow) {
		Integer eventId = newDraftEvent(rows, seatsPerRow);
		workflow.publishEvent(eventId);
		return eventId;
	}

	protected Event event(Integer eventId) {
		return eventRepository.getById(eventId);
	}

	protected List<Integer> seats(Integer... ids) {
		return Arrays.asList(ids);
	}

	/** Runs all tasks on their own thread, released together. Any exception other than a lost race fails the test. */
	protected List<Boolean> race(List<Callable<Boolean>> tasks) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
		List<Future<Boolean>> futures = new ArrayList<>();
		for (Callable<Boolean> task : tasks) {
			futures.add(pool.submit(() -> {
				start.await();
				return task.call();
			}));
		}
		start.countDown();
		List<Boolean> results = new ArrayList<>();
		for (Future<Boolean> future : futures)
			results.add(future.get());
		pool.shutdown();
		return results;
	}
}
