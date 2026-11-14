package org.elide.events.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.elide.events.domain.event.Event;
import org.elide.events.domain.event.EventStatus;
import org.elide.events.domain.identity.UserAccount;
import org.elide.events.domain.identity.UserRole;
import org.elide.events.domain.sales.HoldStatus;
import org.elide.events.domain.sales.Order;
import org.elide.events.domain.sales.OrderStatus;
import org.elide.events.domain.sales.SeatHold;
import org.elide.events.domain.sales.Ticket;
import org.elide.events.domain.venue.Hall;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Spring Data JPA repositories (interfaces extending JpaRepository) against the PostgreSQL database of application.properties:
 * entity mappings, aggregates (cascade / orphanRemoval), custom finders, entity listeners.
 */
public class Test8_JpaRepositoriesTest extends AbstractDomainTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /* ---------------- application.properties + database connection ---------------- */

    @Test
    public void entityManagerFactoryIsConnectedToTheDatabase() {
        assertTrue(entityManagerFactory.isOpen());
        // Event, TicketType, EventSeat, Venue, Hall, Seat, UserAccount, SeatHold, SeatHoldItem, Order, OrderItem, Ticket
        assertEquals(12, entityManagerFactory.getMetamodel().getEntities().size());

        String database = transactionManager.inTransaction(() -> (String) transactionManager.currentEntityManager()
                .createNativeQuery("select current_database()").getSingleResult());
        if (System.getProperty("spring.datasource.url") == null)
            assertEquals("eliade_event", database);
        assertNotNull(database);
    }

    /* ---------------- aggregates: cascade, back-references, ordering ---------------- */

    @Test
    public void hallAggregateIsPersistedWithItsVenueAndSeats() {
        Hall hall = newHall(2, 3);

        assertNotNull(hall.getHallId());
        assertNotNull(hall.getVenue().getVenueId());           // venue cascaded from the hall

        Hall stored = hallRepository.getByIdOrNull(hall.getHallId());
        assertEquals(6, stored.getSeats().size());              // components cascaded from the root
        assertEquals("A1", stored.getSeats().get(0).getLabel()); // @OrderBy keeps the creation order
        assertEquals("B3", stored.getSeats().get(5).getLabel());
        assertTrue(stored.getSeats().stream().allMatch(s -> s.getSeatId() != null && s.getHall().equals(stored)));
        assertEquals(6, stored.getCapacity());
    }

    @Test
    public void eventAggregateIsStoredWithTicketTypesAndSeatsAndReloadedEagerly() {
        Integer eventId = newDraftEvent(2, 5);

        Event stored = eventRepository.getByIdOrNull(eventId);
        assertEquals(EventStatus.DRAFT, stored.getStatus());
        assertEquals(10, stored.getEventSeats().size());
        assertEquals(1, stored.getTicketTypes().size());
        assertEquals(new BigDecimal("100.00"), stored.getBasePrice());
        assertNotNull(stored.getHall().getName());               // eager association: usable after the session is closed
        assertTrue(stored.getEventSeats().stream().allMatch(s -> s.getTicketType() != null && s.getEvent().equals(stored)));
        assertEquals("A1", stored.getEventSeats().get(0).getLabel());
    }

    @Test
    public void componentRemovedFromTheAggregateIsDeletedFromTheDatabase() {
        Integer eventId = newDraftEvent(1, 2);
        transactionManager.runInTransaction(() -> eventRepository.getByIdOrNull(eventId).addTicketType("VIP", new BigDecimal("300.00")));
        assertEquals(2, eventRepository.getByIdOrNull(eventId).getTicketTypes().size());

        // orphanRemoval = true: dropping the component from the root deletes its row
        transactionManager.runInTransaction(() ->
                eventRepository.getByIdOrNull(eventId).getTicketTypes().removeIf(t -> t.getName().equals("VIP")));

        assertEquals(1, eventRepository.getByIdOrNull(eventId).getTicketTypes().size());
    }

    @Test
    public void removingTheRootRemovesTheWholeAggregate() {
        Integer eventId = newDraftEvent(1, 2);
        Integer seatId = event(eventId).getEventSeats().get(0).getEventSeatId();

        assertTrue(eventRepository.remove(eventRepository.getByIdOrNull(eventId)));

        assertNull(eventRepository.getByIdOrNull(eventId));
        Long seatRows = transactionManager.inTransaction(() -> (Long) transactionManager.currentEntityManager()
                .createQuery("select count(s) from EventSeat s where s.eventSeatId = :id").setParameter("id", seatId)
                .getSingleResult());
        assertEquals(0L, seatRows);
        assertFalse(eventRepository.remove(null));
    }

    /* ---------------- repositories: CRUD + custom finders ---------------- */

    @Test
    public void crudRoundTripOnUserAccountRepository() {
        int before = userRepository.size();
        UserAccount user = userRepository.add(factory.buildUser("crud" + System.nanoTime() + "@elide.test", "Ana", "Pop", UserRole.PARTICIPANT));
        assertNotNull(user.getUserId());
        assertEquals(before + 1, userRepository.size());

        user.setFirstName("Ioana");
        userRepository.update(user);
        assertEquals("Ioana", userRepository.getByIdOrNull(user.getUserId()).getFirstName());
        assertTrue(userRepository.toCollection().stream().anyMatch(u -> u.getUserId().equals(user.getUserId())));

        assertTrue(userRepository.remove(user));
        assertNull(userRepository.getByIdOrNull(user.getUserId()));
        assertEquals(before, userRepository.size());
    }

    @Test
    public void enumsAreStoredAsText() {
        UserAccount admin = newUser(UserRole.ADMINISTRATOR);

        String role = transactionManager.inTransaction(() -> (String) transactionManager.currentEntityManager()
                .createNativeQuery("select role from UserAccount where userId = :id").setParameter("id", admin.getUserId())
                .getSingleResult());

        assertEquals("ADMINISTRATOR", role); // @Enumerated(EnumType.STRING)
    }

    @Test
    public void customFindersFilterByPropertyAndByCustomQuery() {
        UserAccount organizer = newUser(UserRole.ORGANIZER);
        Hall hall = newHall(1, 2);
        Integer eventId = workflow.createDraftEvent(organizer.getUserId(), hall.getHallId(), "Festival Unic " + System.nanoTime(),
                inDays(30), inDays(30).plusHours(2), BigDecimal.TEN);

        assertEquals(1, userRepository.findByEmail(organizer.getEmail()).size());
        assertTrue(userRepository.findByRole(UserRole.ORGANIZER).stream().anyMatch(u -> u.getUserId().equals(organizer.getUserId())));
        assertEquals(1, eventRepository.findByOrganizerId(organizer.getUserId()).size());
        assertTrue(eventRepository.findByStatus(EventStatus.DRAFT).stream().anyMatch(e -> e.getEventId().equals(eventId)));
        assertTrue(eventRepository.findByTitleContaining("festival unic").stream().anyMatch(e -> e.getEventId().equals(eventId)));
        assertTrue(hallRepository.findByName("Sala Mare").stream().anyMatch(h -> h.getHallId().equals(hall.getHallId())));
        assertTrue(hallRepository.findByVenueCity("Iasi").stream().anyMatch(h -> h.getHallId().equals(hall.getHallId())));
    }

    @Test
    public void salesRepositoriesFindHoldsOrdersAndTickets() {
        Integer eventId = newPublishedEvent(1, 4);
        UserAccount buyer = newUser(UserRole.PARTICIPANT);

        Integer holdId = workflow.holdSeats(buyer.getUserId(), eventId, seats(eventId, 1, 2));
        assertEquals(eventId, seatHoldRepository.findEventIdByHoldId(holdId));
        assertEquals(2, seatHoldRepository.countSeatsOwnedByUser(buyer.getUserId(), eventId));
        List<SeatHold> active = seatHoldRepository.findByEventIdAndStatus(eventId, HoldStatus.ACTIVE);
        assertEquals(1, active.size());
        assertEquals(2, active.get(0).getItems().size());

        Integer orderId = workflow.createOrder(holdId);
        assertEquals(eventId, orderRepository.findEventIdByOrderId(orderId));
        assertEquals(orderId, orderRepository.findByHoldId(holdId).get(0).getOrderId());
        assertEquals(1, orderRepository.findByUserId(buyer.getUserId()).size());
        assertEquals(1, orderRepository.findByEventIdAndStatus(eventId, OrderStatus.PENDING).size());

        List<Integer> ticketIds = workflow.confirmPayment(orderId);
        assertEquals(OrderStatus.PAID, orderRepository.findByEventId(eventId).get(0).getStatus());
        assertEquals(2, ticketRepository.findByOrderId(orderId).size());
        Ticket ticket = ticketRepository.getByIdOrNull(ticketIds.get(0));
        assertEquals(ticket.getTicketId(), ticketRepository.findByQrToken(ticket.getQrToken()).getTicketId());
        assertNull(ticketRepository.findByQrToken("no-such-token"));
        assertNull(ticketRepository.findByQrToken(null));
        assertTrue(ticket.getTicketCode().startsWith("TKT-" + eventId + "-" + orderId + "-"));
    }

    /* ---------------- entity listeners (computation + audit rules) ---------------- */

    @Test
    public void orderTotalIsComputedByTheEntityListenerOnInsertAndUpdate() {
        Order order = new Order(null, 1, 1, 1, inDays(0), inDays(0).plusMinutes(10));
        order.addItem(1, "A1", "STANDARD", new BigDecimal("100.00"));
        order.addItem(2, "A2", "STANDARD", new BigDecimal("50.00"));
        order.setTotal(new BigDecimal("1.00")); // wrong on purpose

        orderRepository.add(order); // @PrePersist recomputes the total from the items
        assertEquals(new BigDecimal("150.00"), orderRepository.getByIdOrNull(order.getOrderId()).getTotal());
        assertEquals(2, orderRepository.getByIdOrNull(order.getOrderId()).getItems().size());

        transactionManager.runInTransaction(() -> {
            Order managed = orderRepository.getByIdOrNull(order.getOrderId());
            managed.getItems().get(0).setUnitPrice(new BigDecimal("70.00"));
            managed.setCurrency("EUR"); // the order is dirty: @PreUpdate recomputes the total
        });
        assertEquals(new BigDecimal("120.00"), orderRepository.getByIdOrNull(order.getOrderId()).getTotal());
    }
}
