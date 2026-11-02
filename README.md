# Elide Events - documentație pe sprinturi

# Elide Events - sprint 1

## 1. Tema aplicației și funcționalitățile principale
Elide Events funcționează ca o platformă SaaS de ticketing și management de evenimente. Organizatorii pot crea și administra evenimente, stabili categorii de bilete și urmări vânzările, iar participanții pot căuta evenimente, rezerva și cumpăra bilete online. Pentru organizatori, accesul la funcționalitățile avansate (publicare nelimitată de evenimente, configurarea sălilor cu locuri numerotate și statistici detaliate) este oferit pe bază de abonament (subscripție). Încasarea biletelor este procesată securizat și centralizat prin platformă (integrare Stripe), sistemul generând automat biletele cu cod QR unic și eliberând locurile în cazul nefinalizării plății. Administratorii platformei supraveghează activitatea utilizatorilor și validează organizatorii.

Problema pe care o rezolvă aplicația este lipsa unei platforme centralizate pentru gestionarea evenimentelor și monitorizarea biletelor vândute. În multe situații, organizatorii utilizează fișiere Excel sau evidențe manuale, ceea ce poate genera erori privind disponibilitatea biletelor, dificultăți de raportare și procese lente de validare la intrarea în evenimente.

**Funcționalități principale:**
- Gestionarea conturilor de utilizator (participant/client, organizatorul evenimentului, administratorul platformei);
- Publicarea și administrarea evenimentelor (operații CRUD, funcție de vizibilitate a evenimentului public/privat);
- Configurarea categoriilor de bilete și emiterea/generarea acestora;
- Rezervarea și cumpărarea biletelor pentru participanți;
- Rezervarea poate bloca temporar un loc, iar cumpărarea confirmă biletul după procesarea cu succes a plății. Dacă plata eșuează sau rezervarea expiră, locul trebuie eliberat;
- Validarea biletelor prin scanarea codului QR;
- Localizarea evenimentelor pe hartă: Integrare cu servicii de hărți (ex. Google Maps API) pentru afișarea locației exacte a evenimentului și obținerea indicațiilor de orientare;
- Selectarea interactivă a locurilor (Seat Map / Hartă interactivă a sălii): Posibilitatea participanților de a vizualiza sala (rânduri, locuri libere/ocupate/rezervate temporar în stil cinema/teatru) și de a selecta direct locul dorit;
- Rapoarte de vânzări pentru organizatori și rapoarte ale platformei pentru administrator;
- Generarea statisticilor de performanță.

---

## 2. Echipă și roluri

| Rol | Membru |
| :--- | :--- |
| **Product Owner și Business Analyst** | Crudu Ioana-Andreea |
| **Tech Lead și Architect** | Vornicu Denisa-Ștefania |
| **Developer și QA Tester** |  Rusu Elena |

---

## 3. Backlog inițial: Epics
- **Epic 1 – Autentificare și gestionarea conturilor:** Utilizatorii își pot crea conturi și se pot autentifica în aplicație. Sistemul gestionează rolurile de client, organizator și administrator.
- **Epic 2 – Gestionarea evenimentelor:** Organizatorii aprobați pot crea și publica evenimente, iar clienții pot căuta evenimentele publicate.
- **Epic 3 – Rezervări și bilete:** Clienții pot rezerva bilete, iar sistemul gestionează disponibilitatea acestora și expirarea rezervărilor.
- **Epic 4 – Plăți:** Clienții pot plăti rezervările, iar sistemul confirmă plățile și permite emiterea biletelor.
- **Epic 5 – Validarea biletelor:** Sistemul verifică validitatea biletelor prin codul QR și identifică biletele deja utilizate.
- **Epic 6 – Raportare și administrare:** Organizatorii pot consulta rapoartele de vânzări, iar administratorul poate aproba conturile organizatorilor.

---

## 4. User Stories (Gherkin)

### Epic 1 – Autentificare și conturi
* **US 1.1 – Înregistrarea unui participant
• Scenario: Înregistrare reușită
    ◦ Given că sunt un vizitator fără cont
    ◦ When completez datele necesare și trimit formularul
    ◦ Then contul meu este creat cu rolul PARTICIPANT și statusul ACTIVE
    ◦ And primesc un email de confirmare
  
US 1.2 – Autentificarea
• Scenario: Autentificare reușită
    ◦ Given că am un cont cu statusul ACTIVE
    ◦ When introduc datele corecte
    ◦ Then sunt autentificat (access token + refresh token)
• Scenario: Autentificare eșuată
    ◦ Given că am un cont existent
    ◦ When introduc o parolă incorectă
    ◦ Then sistemul afișează un mesaj de eroare
• Scenario: Cont suspendat
    ◦ Given că contul meu are statusul SUSPENDED
    ◦ When încerc să mă autentific
    ◦ Then accesul îmi este refuzat
    
US 1.3 – Deconectarea
• Scenario: Deconectare
    ◦ Given că sunt autentificat
    ◦ When aleg deconectarea
    ◦ Then refresh token-ul meu este invalidat și sesiunea se încheie
    
US 1.4 – Controlul accesului pe rol (RBAC)
• Scenario: Acces interzis
    ◦ Given că sunt autentificat ca participant
    ◦ When apelez o operație de organizator sau de administrator
    ◦ Then serverul răspunde 403 și operația nu se execută

### Epic 2 – Gestionarea evenimentelor
* **US 2.1 – Crearea unui eveniment**
  * *Scenario: Crearea unui eveniment*
    * **Given** că sunt autentificat ca organizator aprobat
    * **When** completez informațiile necesare și salvez evenimentul
    * **Then** evenimentul este creat în sistem
• Scenario: Organizator neactiv
    ◦ Given că contul meu de organizator nu este ACTIVE
    ◦ When încerc să creez un eveniment
    ◦ Then sistemul refuză operația
      
* **US 2.2 – Publicarea unui eveniment**
  * *Scenario: Publicarea unui eveniment*
    * **Given** că am creat un eveniment cu informațiile necesare
    * **When** public evenimentul
    * **Then** evenimentul apare în lista publică
• Scenario: Modificare după publicare
    ◦ Given că evenimentul este PUBLISHED și are locuri vândute sau blocate temporar
    ◦ When încerc să-i schimb data
    ◦ Then sistemul refuză modificarea
      
* **US 2.3 – Căutarea evenimentelor**
  * *Scenario: Căutarea evenimentelor*
    * **Given** că există evenimente publicate
    * **When** caut evenimente după oraș sau dată
    * **Then** sunt afișate evenimentele care corespund criteriilor
      
* **US 2.4 – Localizarea evenimentului pe hartă**
  * *Scenario: Afișare hartă locație*
    * **Given** că accesez detaliile unui eveniment publicat
    * **When** vizualizez secțiunea de adresă/locație
    * **Then** sistemul încarcă harta interactivă cu marcajul poziției evenimentului

### Epic 3 – Rezervări și bilete
* **US 3.1 – Rezervarea biletelor disponibile**
  * *Scenario: Rezervarea biletelor disponibile*
    * **Given** că sunt autentificat și există bilete disponibile
    * **When** rezerv două bilete VIP
    * **Then** rezervarea este creată pentru 10 minute
* **US 3.2 – Expirarea rezervării**
  * *Scenario: Rezervare expirată*
    * **Given** că am o rezervare neplătită
    * **When** expiră perioada de 10 minute
    * **Then** rezervarea este anulată
    * **And** biletele redevin disponibile
* **US 3.3 – Gestionarea biletelor epuizate**
  * *Scenario: Bilete epuizate*
    * **Given** că toate biletele din categoria aleasă sunt indisponibile
    * **When** încerc să rezerv un bilet
    * **Then** sistemul afișează un mesaj că biletele sunt epuizate
* **US 3.4 – Selectarea locului pe harta interactivă a sălii**
  * *Scenario: Selectare loc pe harta sălii*
    * **Given** că evenimentul are alocare de locuri numerotate și există locuri disponibile
    * **When** selectez un scaun marcat ca fiind liber de pe planul sălii
    * **Then** scaunul își schimbă starea în „selectat”
    * **And** locul este rezervat temporar pentru 10 minute
  * *Scenario: Încercare selectare loc deja ocupat/rezervat*
    * **Given** că un loc de pe hartă este deja rezervat sau cumpărat
    * **When** încerc să dau click pe acel loc
    * **Then** sistemul nu îmi permite selectarea lui și afișează că este indisponibil

### Epic 4 – Plăți
* **US 4.1 – Plata unei rezervări**
  * *Scenario: Plată reușită*
    * **Given** că am o rezervare activă
    * **When** procesatorul de plăți confirmă plata
    * **Then** comanda este marcată ca plătită
    * **And** biletele sunt emise
    * **And** primesc confirmarea prin email
* **US 4.2 – Gestionarea unei plăți eșuate**
  * *Scenario: Plată eșuată*
    * **Given** că am o rezervare activă
    * **When** procesatorul de plăți respinge plata
    * **Then** sistemul afișează un mesaj de eroare
    * **And** biletele nu sunt emise

### Epic 5 – Validarea biletelor
* **US 5.1 – Verificarea unui bilet valid**
  * *Scenario: Bilet valid*
    * **Given** că există un bilet valid și neutilizat
    * **When** codul QR este verificat
    * **Then** sistemul confirmă că biletul este valid
* **US 5.2 – Verificarea unui bilet invalid**
  * *Scenario: Bilet invalid*
    * **Given** că un cod QR nu corespunde unui bilet valid
    * **When** codul QR este verificat
    * **Then** sistemul afișează un mesaj că biletul este invalid
* **US 5.3 – Verificarea unui bilet deja utilizat**
  * *Scenario: Bilet deja utilizat*
    * **Given** că biletul a fost deja utilizat
    * **When** codul QR este verificat din nou
    * **Then** sistemul informează că biletul a fost deja utilizat

### Epic 6 – Raportare și administrare
* **US 6.1 – Consultarea raportului de vânzări**
  * *Scenario: Vizualizarea raportului de vânzări*
    * **Given** că sunt autentificat ca organizator al evenimentului
    * **When** accesez raportul evenimentului
    * **Then** sistemul afișează biletele vândute pe categorie și venitul confirmat
* **US 6.2 – Aprobarea unui organizator**
  * *Scenario: Aprobare reușită*
    * **Given** că există un organizator care așteaptă aprobarea
    * **When** aprob contul acestuia
    * **Then** statutul organizatorului devine "Aprobat"

---

## 5. ADR 001 – Alegerea stack-ului tehnologic
* **Status:** Acceptat
* **Context:** Aplicația are logică de business care trebuie respectată strict (stoc limitat de bilete, expirarea rezervărilor, validare unică la intrare) și trebuie să fie organizată pe straturi separate: prezentare, logică de business, acces la date. Avem nevoie de un stack care permite tranzacții sigure, teste automate și CI/CD.

| Criteriu | Spring Boot + PostgreSQL + React | .NET + SQL Server + Angular | Node.js + MongoDB + Vue.js |
| :--- | :--- | :--- | :--- |
| **Arhitectură pe straturi** | Foarte bună (controller / service / repository) | Foarte bună | Depinde de disciplina echipei |
| **Tranzacții și consistență** | Excelentă (ACID) | Excelentă (ACID) | Mai slabă pentru stocuri și plăți |
| **Cost licențe** | Zero | Licențe SQL Server | Zero |
| **Cunoștințe în echipă** | Bune | Parțiale | Parțiale |
| **Testare și CI/CD** | JUnit, Testcontainers, GitHub Actions | xUnit, Azure DevOps | Jest, GitHub Actions |

* **Decizie:** Se alege **Java + Spring Boot + PostgreSQL + React**.
* **Motive:**
  - Vânzarea de bilete cere tranzacții și blocări corecte, ca două persoane să nu cumpere același loc. O bază relațională precum PostgreSQL rezolvă asta mult mai sigur decât MongoDB.
  - Spring Boot impune natural separarea pe straturi, ceea ce se potrivește cu cerințele proiectului.
  - Tehnologiile alese sunt open-source, eliminând necesitatea costurilor legate de licențiere.
  - React permite o interfață modernă, cu componente reutilizabile (listă evenimente, selector de locuri, pagină de checkout).
  - Testele automate (JUnit, Testcontainers pentru PostgreSQL) și pipeline-ul CI/CD se integrează simplu.
* **Consecințe:**
  - *Pozitive:* Curbă de învățare mică, documentație bogată, comunitate mare, deploy ușor prin Docker sau Vercel.
  - *Negative:* Configurarea inițială a Spring Boot este mai minuțioasă decât la Node, deoarece frontend-ul și backend-ul se dezvoltă în două proiecte separate, cu un contract REST de menținut (documentat cu OpenAPI).

---

## 6. Repository GitHub
Repository-ul GitHub se poate găsi accesând:  
[https://github.com/EleR03/DAM_Project.git](https://github.com/EleR03/DAM_Project.git)

---

## 7. Tool management taskuri
Am ales platforma **Trello** pentru organizarea și monitorizarea activităților echipei, deoarece oferă o interfață vizuală simplă, bazată pe panouri, liste și carduri.

Vom utiliza un board dedicat proiectului Elide Events, organizat în următoarele liste:
- **Sarcini inițiale și descrierea/stabilirea obiectivelor sprinturilor**
- **Arhitectură și documentație**
- **Product Backlog:** Descrierea Epics și User Stories identificate.
- **Diagrame / Revizuiri:** Sarcinile care necesită verificare.
- **Done:** Sarcinile finalizate și acceptate.

Fiecare card va conține descrierea sarcinii, persoana responsabilă, prioritatea, criteriile de acceptare și, dacă este cazul, estimarea în Story Points.  
Trello va facilita distribuirea responsabilităților, urmărirea progresului și identificarea sarcinilor blocate.

🔗 **Link Board Trello:** [Sprintul 1 – Inițierea proiectului Elide Events](https://trello.com/invite/b/6ad4e975db36dbbb11e3a90d/ATTI2ed2706e8eb02df02c436a7956ef80130F25A5A1/sprintul-1-inițierea-proiectului-elide-events)

---

## 8. Diagrama Use Case
Actori: Participant, Organizator, Administrator. Validarea biletelor la intrare o face organizatorul (permisiunea TICKET_VALIDATE); nu există un actor separat „Personal de intrare”.
Sisteme externe: Stripe (plăți bilete, în Test Mode, și abonamente prin Stripe Billing, cu webhook-uri); SendGrid (emailuri); Google Maps Platform (Maps JavaScript API pentru hartă, Places Autocomplete pentru alegerea locației, Geocoding API pentru adresă → coordonate). OpenStreetMap/MapLibre nu mai este folosit, iar Netopia nu face parte din proiect.
Fluxul participantului: cont (confirmat prin email), autentificare, căutare, detalii eveniment cu harta locației, Selectare locuri pe harta sălii și blocarea lor 10 minute, comandă, plată Stripe, apoi emiterea biletelor cu QR și trimiterea lor pe email.
Relații:
• «include»: Vizualizare detalii → Afișare locație pe hartă; Înregistrare → Trimitere email; Plată rezervare → Procesare plată Stripe; Creare/editare eveniment → Geocodare adresă; Publicare eveniment → Verificare organizator aprobat; Selectare locuri → Blocare locuri 10 minute.
• «extend» (comportament condiționat): Emitere bilete cu QR extinde plata, condiția fiind confirmarea prin webhook; Gestionare plată eșuată extinde plata, condiția fiind respingerea; Eliberare locuri la expirare extinde blocarea, condiția fiind depășirea celor 10 minute (rulată de jobul periodic, fără actor); Refuzare bilet invalid/utilizat extinde validarea.
• Selectarea locurilor este «include» față de rezervare, deoarece în modelul curent orice eveniment are o sală cu locuri (vezi punctul de decizie D1).
• Abonamentul (FREE_TRIAL/MONTHLY) este funcționalitate de bază, nu „extensie opțională”: Activare abonament «include» Procesare plată abonament (Stripe Billing).
• Administratorul aprobă/respinge organizatori, gestionează utilizatori și permisiuni, vede abonamentele, plățile, auditul și KPI.

## 9. Diagrama C4
Nivel 1: Context
Elide Events este un singur sistem folosit prin HTTPS de Participant, Organizator și Administrator, care depinde de trei servicii externe: Stripe, SendGrid și Google Maps Platform. Organizatorul lucrează pe planul FREE_TRIAL sau MONTHLY.
Nivel 2: Containere
• Aplicația web (SPA, React + TypeScript): listă evenimente, selector de locuri, checkout, hartă, panouri organizator/administrator. Apelează API-ul prin JSON/HTTPS și încarcă harta prin Maps JavaScript API.
• API REST (Java 17, Spring Boot): straturi controller, service, repository; autentificare cu access/refresh token și RBAC pe server; evenimente, săli, locuri, blocări de 10 minute, comenzi, plăți, emiterea și validarea biletelor, abonamente, rapoarte, audit. Creează plățile Stripe și primește webhook-urile, trimite emailuri prin SendGrid și apelează Geocoding API. Un job periodic (la 30 de secunde) eliberează blocările expirate.
• Baza de date (PostgreSQL): toate datele; tranzacțiile ACID și row locking garantează că un loc nu se vinde de două ori.
• Redis (opțional): cache și blocări temporare.


## 10. Diagrama UML (clase de domeniu)
Modelul este organizat pe șapte zone și folosește denumirile din cod (engleză).
Identitate și organizatori. UserAccount (cod: userId, email, firstName, lastName, role, status) cu UserRole = PARTICIPANT, ORGANIZER, ADMINISTRATOR și AccountStatus = PENDING, ACTIVE, SUSPENDED, DELETED, REJECTED. În modelul țintă RBAC este User → Role → Permission (tabele Role, Permission, RolePermission). Profilurile ParticipantProfile, OrganizerProfile (legat de Company), AdminProfile; OrganizerVerification (PENDING, UNDER_REVIEW, APPROVED, REJECTED) cu Document.
Evenimente. Event (agregat rădăcină): organizerId, title, description, status, visibility, startAt, endAt, publishedAt, basePrice, currency, hall. EventStatus = DRAFT, PENDING_PUBLICATION, PUBLISHED, IN_PROGRESS, COMPLETED, CANCELLED, DELETED. Conține TicketType (name, price; în modelul țintă și quantity, sold_quantity, perioadă de vânzare) și EventSeat. Câmpurile calculate availableCount, heldCount, soldCount, blockedCount și occupancyRate = vândute / capacitate × 100 (2 zecimale) sunt tranziente.
Locații și săli. Venue (name, address, city, latitude, longitude, place_id) conține Hall; Hall (agregat rădăcină) conține Seat (rowLabel, seatNumber, label derivat, accessible, active; în modelul țintă și x, y, zonă). EventSeat este locul unei săli pentru un anumit eveniment: status SeatStatus = AVAILABLE, HELD, SOLD, BLOCKED, price, ticketType, holdId, heldUntil. Tranziții permise: AVAILABLE → HELD → SOLD, HELD → AVAILABLE, AVAILABLE → BLOCKED. EventSeat înlocuiește vechea clasă LocEveniment.
Blocare, comandă, plată, bilet. SeatHold (userId, eventId, expiresAt, status ACTIVE/EXPIRED/CONVERTED/CANCELLED) cu SeatHoldItem → Order (tabela orders; status PENDING, PAYMENT_PROCESSING, PAID, FAILED, EXPIRED, CANCELLED, REFUNDED; total calculat) cu OrderItem (eventSeatId, seatLabel, ticketTypeName, unitPrice) → Ticket (ticketCode, qrToken aleator, status ISSUED/USED/CANCELLED/REFUNDED/EXPIRED, issuedAt, usedAt). Payment (provider, provider_payment_id, amount, status) aparține comenzii; TicketScan (rezultat TicketScanResult: VALID, ALREADY_USED, INVALID, WRONG_EVENT, CANCELLED) păstrează scanările.
Abonamente. SubscriptionPlan (FREE_TRIAL, MONTHLY) și OrganizerSubscription cu status TRIAL, ACTIVE, PAST_DUE, GRACE_PERIOD, SUSPENDED, CANCELLED, EXPIRED; SubscriptionPayment, Invoice.
Administrare. AuditLog, Notification.
Utilitare de domeniu. EventSalesView (view de raport: capacitate, disponibile, blocate, vândute, scanate, neprezentate, venit brut, grad de ocupare) și TicketValidationView (rezultatul scanării).
Relații: o comandă conține elemente, iar fiecare element generează un bilet; biletul nu este compus în rezervare/comandă, ci legat de OrderItem. Un loc fizic poate apărea în mai multe evenimente prin EventSeat.

## 11. Diagrama SQL (PostgreSQL)
Moștenirea conturilor: tabel utilizator/user cu coloanele comune și tip_utilizator, plus tabelele de profil cu PK = FK (joined table). Statusul contului este coloană separată de rol.
Tabele, pe zone: identitate (user, role, permission, role_permission, refresh_token, password_reset_token); organizatori (organizer_profile, company, organizer_verification, document); evenimente (event, event_category, event_location, ticket_type); săli (venue, hall, seat_zone, seat, event_seat); vânzări (seat_hold, seat_hold_item, orders, order_item, payment, payment_transaction); bilete (ticket, ticket_scan); abonamente (subscription_plan, organizer_subscription, subscription_payment, invoice); administrare (audit_log, notification, favorite, event_view).
Constrângeri esențiale: UNIQUE pe email, cod fiscal, qr_token, ticket_code, provider_payment_id și (event_id, seat_id) în event_seat; ticket.event_seat_id unic pentru bilete active; sume NUMERIC(10,2); momente TIMESTAMP; coloane de stare VARCHAR cu CHECK, cu aceleași valori ca enumerările din cod. Tabela comenzilor se numește orders (cuvânt rezervat).

---

# Elide Events – Backend (Sprint 2: Domain Model & Business Logic Components)

**Stack:** Java 17 | Spring Boot 2.7 | Maven | JUnit 5 | (PostgreSQL, Redis, React în sprinturile următoare)  
**Obiectiv Sprint 2:** Implementarea completă a modelului de domeniu (DDD), a serviciilor de suport (Factory & Repositories in-memory), serviciilor de calcul și a fațadei de workflow tranzacțional, acoperite integral de 70 de teste automate. Fără dependență de o bază de date SQL externă sau frontend.

---

## 1. Managementul Proiectului & Documentare Trello

Planificarea, urmărirea task-urilor și colaborarea din cadrul echipei pentru **Sprintul 2** s-au desfășurat prin intermediul platformei **Trello**:

* **Organizare pe Coloane Agile/Scrum:**
  * **Product Backlog:** Cerințele generale extrase din documentația de analiză (`APOO-Proiect`) și din cerințele Cursului 4 (DAM).
  * **Sprint Backlog (Sprint 2):** Împărțirea pe sarcini atomice: agregate DDD, fabrică de entități, validare cu adnotări JSR 349, servicii de calcul, fațadă de workflow, mecanism de locking concurent, politica de anulare/compensare și testele de stres.
  * **In Progress / In Review:** Lucrul colaborativ pe ramuri și validarea reciprocă a deciziilor arhitecturale (ex. diferențierea `cancelEvent` cu compensare automată față de `deleteEvent` cu blocare fiscală).
  * **Done (Acceptance Criteria Verified):** Toate cele 7 suite de teste trec cu succes (`mvn test`), verificând regulile funcționale și nefuncționale (SLA-uri de concurență).

🔗 **Link Board Trello:** https://trello.com/b/xBhmPq6n/sprintul-2-model-de-domeniu-%C8%99i-servicii-de-business
---

## 2. Structura Proiectului

Proiectul respectă arhitectura pe pachete standardizată pentru componentele de domeniu:

```text
elide-events-backend/
├── pom.xml                                      # Configurare Maven, dependențe Spring Boot & JUnit 5
├── README.md                                    # Documentația completă a Sprintului 2
└── src/
    ├── main/
    │   ├── java/org/elide/events/
    │   │   ├── ElideEventsApplication.java      # Clasa principală de bootstrap Spring Boot
    │   │   ├── spring/
    │   │   │   └── AppScanConfig.java           # Configurare Spring IoC (@Configuration, @ComponentScan, Validator)
    │   │   └── domain/
    │   │       ├── identity/                    # Modulul de Identitate și Securitate
    │   │       │   ├── AccountStatus.java       # Enum: ACTIVE, PENDING, SUSPENDED
    │   │       │   ├── UserAccount.java         # Entitate / Agregat Utilizator
    │   │       │   └── UserRole.java            # Enum: PARTICIPANT, ORGANIZER, ADMIN
    │   │       ├── venue/                       # Modulul de Administrare Săli / Locații
    │   │       │   ├── Hall.java                # Agregat Rădăcină Sală
    │   │       │   ├── Seat.java                # Entitate internă Scaun (șablon sală)
    │   │       │   └── Venue.java               # Entitate Locație fizică
    │   │       ├── event/                       # Modulul de Evenimente
    │   │       │   ├── Event.java               # Agregat Rădăcină Eveniment
    │   │       │   ├── EventSeat.java           # Entitate internă Loc Eveniment (AVAILABLE, HELD, SOLD, BLOCKED)
    │   │       │   ├── TicketType.java          # Entitate internă Categorie de bilet (VIP, Standard)
    │   │       │   ├── EventStatus.java         # Enum: DRAFT, PUBLISHED, CANCELLED, COMPLETED
    │   │       │   ├── SeatStatus.java          # Enum stări loc
    │   │       │   └── EventSalesView.java      # DTO Consolidat de Raportare Vânzări
    │   │       ├── sales/                       # Modulul Tranzacțional de Vânzare și Bilete
    │   │       │   ├── Order.java               # Agregat Rădăcină Comandă
    │   │       │   ├── OrderItem.java           # Entitate internă Linie de comandă
    │   │       │   ├── OrderStatus.java         # Enum: PENDING, PAYMENT_PROCESSING, PAID, FAILED, EXPIRED, CANCELLED, REFUNDED
    │   │       │   ├── SeatHold.java            # Agregat Rădăcină Blocare Temporară (10 min)
    │   │       │   ├── SeatHoldItem.java        # Entitate internă Loc reținut
    │   │       │   ├── HoldStatus.java          # Enum: ACTIVE, RELEASED, EXPIRED, CONVERTED, CANCELLED
    │   │       │   ├── Ticket.java              # Agregat Bilet (conține token QR criptografic)
    │   │       │   ├── TicketStatus.java        # Enum: ISSUED, USED, CANCELLED, REFUNDED
    │   │       │   ├── TicketScanResult.java    # Enum Scanare: VALID, ALREADY_USED, INVALID, WRONG_EVENT, CANCELLED
    │   │       │   └── TicketValidationView.java# DTO Răspuns scanare la poartă
    │   │       └── services/                    # Servicii de Domeniu și Suport
    │   │           ├── IEntityRepository.java
    │   │           ├── InMemoryEntityRepositoryBase.java # Suport generic stocare in-memory
    │   │           ├── *RepositoryBase.java     # Implementări repository pentru fiecare rădăcină
    │   │           ├── IEventEntityFactory.java & EventEntityFactoryBase.java # Șablon Factory
    │   │           ├── IValidatingEventDomainService.java & ValidatingEventDomainServiceImpl.java
    │   │           ├── ISummarizingEventDomainService.java & SummarizingEventDomainServiceImpl.java
    │   │           ├── IPricingOrderDomainService.java & PricingOrderDomainServiceImpl.java
    │   │           ├── IConsolidatingEventSalesViewDomainService.java & ConsolidatingEventSalesViewDomainServiceImpl.java
    │   │           ├── IEventTicketingBusinessWorkflowService.java & EventTicketingBusinessWorkflowServiceImpl.java # Facade
    │   │           └── Excepții de Domeniu:
    │   │               ├── BusinessRuleViolationException.java
    │   │               ├── DomainValidationException.java
    │   │               ├── SeatNoLongerAvailableException.java
    │   │               ├── TicketLimitExceededException.java
    │   │               └── PaymentRejectedException.java
    │   └── resources/                           # Gol conform specificațiilor Sprint 2 (fără DB SQL externă)
    └── test/java/org/elide/events/domain/services/
        ├── AbstractDomainTest.java              # Clasă de bază pentru teste (DI, helper methods, reset date)
        ├── Test1_EntityFactoryTest.java         # Teste instanțiere fabrică agregate (4 teste)
        ├── Test2_ValidatingDomainServiceTest.java # Teste Bean Validation JSR 349 (10 teste)
        ├── Test3_ComputingDomainServicesTest.java # Teste calcul câmpuri și DTO-uri agregate (5 teste)
        ├── Test4_BusinessWorkflowServiceTest.java # Teste flux ciclu de viață E2E (27 teste)
        ├── Test5_ConcurrencyTest.java           # Teste SLA și concurență multithreading (5 teste)
        ├── Test6_EventCancellationTest.java     # Teste politică Cancel vs Delete (11 teste)
        └── Test7_TicketLimitTest.java           # Teste invariant anti-scalping 8 bilete (8 teste)
```

---

## 3. Maparea pe Șabloanele Cursului 4 (DAM)

| Șablon Teoretic (Cursul 4) | Rol Arhitectural | Implementare în Elide Events |
| :--- | :--- | :--- |
| **Domain Model & Aggregates** | Încapsularea stărilor și consistența tranzacțională | Rădăcini: `Event`, `Hall`, `Order`, `SeatHold`, `Ticket`, `UserAccount`. Entități interne: `EventSeat`, `TicketType`, `Seat`, `OrderItem`, `SeatHoldItem`. |
| **Value Objects (Enum-uri)** | Reprezentarea imutabilă a stărilor mașinii de stări | `EventStatus`, `SeatStatus`, `HoldStatus`, `OrderStatus`, `TicketStatus`, `TicketScanResult`, `UserRole`, `AccountStatus`. |
| **Entity Support: Factory** | Instanțierea completă și validă a agregatelor complexe | `IEventEntityFactory` & `EventEntityFactoryBase` (`buildHall`, `buildEventWithSeats`, `buildUser`). |
| **Entity Support: Repository** | Decuplarea stocării de logica aplicativă | `InMemoryEntityRepositoryBase<T, ID>` (Map concurrent) + interfețe dedicate (`IEventRepository`, `ITicketRepository` etc.). |
| **Domain Validation Service** | Validare automată pe baza adnotărilor JSR 349 | `IValidatingEventDomainService` & `ValidatingEventDomainServiceImpl` (verificare Bean Validation + invariant compus de agregat). |
| **Domain Computation Services** | Calculul valorilor derivate și agregarea de rapoarte | `SummarizingEventDomainServiceImpl` (locuri, grad ocupare %), `PricingOrderDomainServiceImpl` (total comandă), `ConsolidatingEventSalesViewDomainServiceImpl` (`EventSalesView`). |
| **Business Workflow Service** | Șablon Fațadă (Orchestrator de use-case-uri) | `IEventTicketingBusinessWorkflowService` & `EventTicketingBusinessWorkflowServiceImpl` (`@Qualifier("EventTicketingBusinessWorkflowServiceFacade")`). |
| **Spring IoC Configuration** | Asamblarea containerului de dependențe | `AppScanConfig` (`@Configuration`, `@ComponentScan`, bean-urile `Validator` și `Clock`). |

---

## 4. Reguli de Business & Decizii de Design

1. **Validare cu Adnotări (JSR 349 / Bean Validation):**
   * `@NotNull` și `@Size(min = 3, max = 200)` pe titlul evenimentului.
   * `@Future` pe data de început a spectacolului (`startAt`), împiedicând crearea sau publicarea evenimentelor în trecut.
   * Invariant compus la nivel de agregat: `endAt > startAt`, existența a cel puțin unui loc și a unei categorii de preț la publicare, comenzi nenule cu cel puțin un bilet.
2. **Calcul și Aritmetică:**
   * **Ocupare:** $\text{occupancyRate} = \frac{\text{vândute}}{\text{capacitate}} \times 100$, cu rotunjire strictă la 2 zecimale (`RoundingMode.HALF_UP`).
   * **Total Comandă:** Calculat ca sumă a prețurilor biletelor adăugate în coș (`sum(unitPrice)`).
3. **Mecanismul de Rezervare (Hold) & Anti-Scalping:**
   * Blocare temporară pe **10 minute** (`expiresAt = createdAt + 10 min`).
   * Politică **All-or-Nothing**: dacă un singur loc din cele cerute este indisponibil, întreaga operațiune eșuează fără efecte laterale (`SeatNoLongerAvailableException`).
   * Cota maximă de bilete: utilizatorul nu poate deține mai mult de **8 bilete** (cumulativ locuri active în hold + bilete deja plătite) pentru același spectacol (`TicketLimitExceededException`).
4. **Fluxul de Plată în Doi Pași (Two-Step Payment):**
   * `startPayment(orderId)`: Verifică dacă rezervarea de 10 minute este încă activă. Dacă a expirat, plata este respinsă înainte de contactarea băncii (`PaymentRejectedException`). Dacă este validă, comanda trece în `PAYMENT_PROCESSING`, oferind o perioadă de grație de 5 minute pentru debitarea cardului.
   * `confirmPayment(orderId)`: Locurile devin `SOLD`, se generează biletele finale cu cod QR securizat, iar comanda devine `PAID`.
   * `failPayment(orderId)`: Locurile sunt eliberate imediat (`AVAILABLE`).
5. **Politica Operațională: Anulare (`cancelEvent`) vs. Ștergere (`deleteEvent`):**
   * **`deleteEvent`:** Permis **exclusiv** dacă nu există niciun bilet vândut (`soldCount == 0`). Protejează integritatea datelor contabile și auditul financiar.
   * **`cancelEvent`:** Permis oricând înainte ca evenimentul să fie finalizat (`COMPLETED`), chiar dacă există sute de bilete vândute (artist bolnav, motive de forță majoră). Declansează automat un **flux tranzacțional de compensare**:
     * Evenimentul devine `CANCELLED`.
     * Toate rezervările active sunt anulate, iar locurile `HELD` redevin `AVAILABLE`.
     * Biletele emise (`ISSUED`) trec în `REFUNDED` (la intrare scanerul răspunde instant `TicketScanResult.CANCELLED`).
     * Comenzile plătite trec în `REFUNDED`, iar metoda returnează lista ID-urilor lor pentru ca procesatorul de plăți (Stripe) să emită rambursările bancare.
6. **Thread-Safety și Concurență Extremă (SLA):**
   * Toate operațiunile critice de verificare și modificare a stării locurilor și a biletelor QR sunt protejate prin bloc tranzacțional `synchronized (seatLock)`, garantând consistența tranzacțiilor chiar și sub asaltul a sute de cereri paralele.

---

## 5. Suita de Teste Automate (70 de teste JUnit 5)

Rularea completă a suitei de teste se face prin comanda:

```bash
mvn clean test
```

### Detalierea claselor de test:

* **`Test1_EntityFactoryTest` (4 teste):**
  * Verifică generarea corectă a sălilor cu rânduri și locuri.
  * Validează agregarea evenimentului cu locuri inițiale `AVAILABLE` și categorie `STANDARD`.
  * Verifică configurarea de prețuri diferite pe rânduri specifice (ex. rândul VIP).
  * Asigură generarea de ID-uri unice pentru toate entitățile.

* **`Test2_ValidatingDomainServiceTest` (10 teste):**
  * Verifică validările Bean Validation: lungime titlu, `@Future` pe `startAt`, preț nenegativ.
  * Verifică validarea în cascadă cu `@Valid` pe categoriile de bilete.
  * Testează invariantul `endAt > startAt` și respingerea evenimentelor fără locuri sau a comenzilor goale (`DomainValidationException`).

* **`Test3_ComputingDomainServicesTest` (5 teste):**
  * Testează numărarea dinamică a locurilor pe stări (`available`, `held`, `sold`, `blocked`).
  * Validează formula procentuală a gradului de ocupare și rotunjirea la 2 zecimale.
  * Verifică calculul corect al totalului comenzii (`sum(unitPrice)`).
  * Testează agregarea raportului complet `EventSalesView`.

* **`Test4_BusinessWorkflowServiceTest` (27 teste):**
  * Verifică controlul accesului RBAC și publicarea evenimentelor (doar organizatorul aprobat poate publica).
  * Testează reprogramarea spectacolelor (blocată dacă există vânzări).
  * Verifică modificarea capacității pe categorii (blocată dacă se încearcă tăierea sub numărul de bilete deja vândute/rezervate).
  * Validează întregul ciclu: `holdSeats` $\rightarrow$ expirare la 10 minute $\rightarrow$ eliberare automată $\rightarrow$ `startPayment` $\rightarrow$ `confirmPayment` $\rightarrow$ generare bilet QR.
  * Testează scanarea la intrare acoperind toate cazurile: `VALID`, `ALREADY_USED`, `INVALID`, `WRONG_EVENT`.

* **`Test5_ConcurrencyTest` (5 teste de stres multithreading):**
  * `twoHundredThreadsOnTheSameSeatOnlyOneWins`: **200 de thread-uri concurente** blocate la un start comun (`CountDownLatch`) cer exact același loc; exact 1 reușește, iar 199 primesc `SeatNoLongerAvailableException`.
  * `fiveHundredRequestsForTheLastFiftySeatsExactlyFiftySucceed`: **500 de cereri concurente** pentru ultimele 50 de locuri; exact 50 de comenzi reușesc, zero locuri vândute dublu, ocupare 100%.
  * `oneUserTakingFiveOfEightSeatsIsAllOrNothing`: Tranzacție atomică pentru coșuri multiple (toate cele 5 locuri sau niciunul).
  * `twentyGatesScanningTheSameTicketOnlyOneIsValid`: **20 de scanere/turnicheți simultani** încearcă scanarea aceleiași clone de cod QR în aceeași milisecundă; exact 1 scanare primește `VALID`, celelalte 19 primesc `ALREADY_USED`.

* **`Test6_EventCancellationTest` (11 teste):**
  * Demonstrează că `deleteEvent` aruncă excepție dacă există bilete vândute.
  * Demonstrează că `cancelEvent` declanșează compensarea în cascadă: locurile devin libere, biletele devin `REFUNDED`, iar comenzile devin `REFUNDED`.
  * Verifică idempotența anulării și imposibilitatea anulării unui eveniment deja desfășurat sau finalizat (`COMPLETED`).

* **`Test7_TicketLimitTest` (8 teste):**
  * Verifică limita strictă de maximum 8 bilete per utilizator pe spectacol (`TicketLimitExceededException`).
  * Validează caracterul cumulativ (locuri blocate în hold + bilete deja plătite).
  * Verifică eliberarea cotei dacă o plată eșuează sau rezervarea expiră.
  * Testează rezistența limitei de 8 bilete împotriva a 20 de thread-uri paralele ale aceluiași utilizator.

---

## 6. Livrabile Planificate pentru Sprinturile Următoare

* **Sprintul 3:**
  * **Persistență PostgreSQL:** Activarea dependențelor Spring Data JPA (deja adnotate pe entități) și înlocuirea repository-urilor in-memory cu interfețe `JpaRepository`.
  * **Row-Level Locking:** Migrarea de la sincronizarea în memorie (`synchronized`) la tranzacții ACID cu *Pessimistic Locking* (`SELECT FOR UPDATE`).
  * **API RESTful:** Expunerea endpoint-urilor REST (Controllere, DTO-uri de transfer, validare HTTP).
  * **Securitate:** Spring Security + JSON Web Tokens (JWT) pentru protecția rutelor bazată pe roluri (RBAC).
* **Sprintul 4:**
  * **Procesare Plăți:** Integrare Stripe API + procesare securizată a webhook-urilor pentru plata biletelor.
  * **Caching & Rate Limiting:** Redis pentru stocarea rapidă a inventarului de locuri și prevenirea atacurilor de tip DoS.
  * **Frontend Client & Scanner:** Aplicație web SPA dezvoltată în React (interfață de selecție interactivă a sălii, panou organizator și scanner QR mobil pentru personalul de la porțile de acces).
