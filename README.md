# Elide Events

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
