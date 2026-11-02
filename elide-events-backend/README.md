# Elide Events – Backend (Sprint 2: Domain Model + Business Logic)

Stack: **Java 17 + Spring Boot 2.7 + (PostgreSQL + React în sprinturile următoare)**.
Sprint 2 = domain model, servicii de domeniu și teste. **Fără persistență** (repository-uri in-memory) și **fără frontend**.

## Rulare
```bash
mvn test          # rulează toate testurile JUnit 5
```

## Maparea pe template-ul de curs (msd.t2_c4.spring_domain)

| Template (Scrum)                                   | Elide Events                                              |
|----------------------------------------------------|-----------------------------------------------------------|
| `Project` (root) + `Release`, `Feature`            | `Event` (root) + `TicketType`, `EventSeat`                |
| `Team` (root) + `Member`                           | `Hall` (root) + `Seat`; `UserAccount` (entitate)          |
| `Sprint` + `Task`                                  | `SeatHold` + `SeatHoldItem`; `Order` + `OrderItem`; `Ticket` |
| Value objects (enum-uri)                           | `EventStatus`, `SeatStatus`, `HoldStatus`, `OrderStatus`, `TicketStatus`, `TicketScanResult`, `UserRole`, `AccountStatus` |
| `ProjectEntityFactoryBase`                         | `EventEntityFactoryBase` (+ `IEventEntityFactory`)        |
| `ProjectEntityRepositoryBase`                      | `*RepositoryBase` (in-memory, `IEntityRepository<T>`)     |
| `ValidatingProjectDomainService`                   | `ValidatingEventDomainServiceImpl`                        |
| `SummarizingProjectDomainService`                  | `SummarizingEventDomainServiceImpl` (câmpuri calculate)   |
| `ConsolidatingProjectCurrentReleaseView...`        | `ConsolidatingEventSalesViewDomainServiceImpl` (`EventSalesView`) |
| `PlanningProjectBusinessWorkflowService` (Facade)  | `EventTicketingBusinessWorkflowServiceImpl`               |
| `AppScanConfig`                                    | `org.elide.events.spring.AppScanConfig`                   |
| `Test1..Test6`                                     | `Test1..Test7` din `src/test` (Test5 = concurență: 200 thread-uri pe același loc, 500 cereri pentru ultimele 50 de locuri, 5 din 8 locuri rămase, 20 de porți care scanează același bilet; Test6 = anulare vs ștergere eveniment; Test7 = limita de bilete per utilizator)                             |

## Agregate
- **Event** – root; componente: `TicketType`, `EventSeat` (loc pentru UN eveniment: AVAILABLE/HELD/SOLD/BLOCKED).
- **Hall** – root; componente: `Seat`. `Venue` e entitate referită.
- **SeatHold** – root; componente: `SeatHoldItem` (blocare de 10 minute).
- **Order** – root; componente: `OrderItem`. `Ticket` – entitate (QR token aleator).

## Reguli de business implementate
**Validare** (`IValidatingEventDomainService`): anotări JavaBean Validation pe `Event`, `TicketType`, `Order`, `OrderItem`
(titlu 3–200 caractere, `@Future` pe start, end > start, preț ≥ 0, ≥ 1 loc și ≥ 1 tip de bilet, comandă cu ≥ 1 loc) + regulă de agregat.

**Calcul** (`ISummarizing…`, `IPricing…`, `IConsolidating…`):
- `Event`: availableCount / heldCount / soldCount / blockedCount, `occupancyRate = vândute / capacitate × 100` (2 zecimale)
- `Order`: total = suma prețurilor locurilor
- view `EventSalesView`: capacitate, vândute, scanate, neprezentate, venit brut, grad ocupare

**Workflow** (`IEventTicketingBusinessWorkflowService`):
0. `approveOrganizer` (administratorul aprobă organizatorul: PENDING → ACTIVE)
1. `createDraftEvent` (organizator ACTIV sau PENDING; ciorna se poate pregăti înainte de aprobare) → 2. `publishEvent` (**blocat dacă organizatorul nu are contul aprobat**) / `reduceCategoryCapacity` (capacitatea unei categorii nu scade sub biletele vândute + rezervate) / `rescheduleEvent` (după publicare, fără modificare dacă există locuri vândute/blocate)
3. `holdSeats` (10 min, tot-sau-nimic, `SeatNoLongerAvailableException` = 409; maxim `MAX_TICKETS_PER_USER` = 8 bilete/utilizator/eveniment, rezervate + cumpărate, `TicketLimitExceededException`) → 4. `createOrder`
5. `startPayment` (**înainte de a încasa cardul**: dacă cele 10 minute au expirat, plata e respinsă cu `PaymentRejectedException` și banii nu se extrag; altfel comanda trece în PAYMENT_PROCESSING și locurile sunt protejate încă `PAYMENT_GRACE_MINUTES` = 5 min) → `confirmPayment` (locuri SOLD + bilete cu QR) / `failPayment` (locuri eliberate) → 6. `expireHolds` (jobul periodic)
7. `validateTicket` (VALID / ALREADY_USED / INVALID / WRONG_EVENT / CANCELLED) → 8. `getEventSalesReport`

**Anulare vs. ștergere eveniment**
- `cancelEvent` (operațional): permis oricând înainte de COMPLETED, **indiferent de biletele vândute**. Idempotent. Flux de compensare:
  evenimentul → CANCELLED; rezervările active și locurile HELD sunt eliberate; comenzile PENDING → CANCELLED;
  biletele ISSUED → REFUNDED (scanerul răspunde CANCELLED); comenzile PAID → REFUNDED și se returnează lista lor, ca stratul de plăți (Stripe, sprint viitor) să trimită rambursările.
  Nu se anulează un eveniment COMPLETED sau care s-a terminat deja.
- `deleteEvent` (ștergere de date): permis doar dacă **nu există bilete vândute** (altfel se pierde evidența financiară); rezervările active sunt eliberate.

## Ce rămâne pentru sprinturile următoare
- Persistență PostgreSQL (în `pom.xml` sunt dependențele comentate; entitățile au deja anotări JPA, `Order` → tabela `orders`).
  Blocarea locurilor va deveni tranzacție + row locking; acum e un `synchronized` în workflow.
- Controllere REST, RBAC pe server, Stripe + webhook, Redis, React.
