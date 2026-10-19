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
| **Product Owner și Business Analyst** | Rusu Elena |
| **Tech Lead și Architect** | Vornicu Denisa-Ștefania |
| **Developer și QA Tester** | Crudu Ioana-Andreea |

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
* **US 1.1 – Înregistrarea unui client**
  * *Scenario: Înregistrare reușită*
    * **Given** că sunt un vizitator fără cont
    * **When** completez datele necesare și trimit formularul
    * **Then** contul meu este creat
    * **And** primesc un email de confirmare
* **US 1.2 – Autentificarea utilizatorului**
  * *Scenario: Autentificare reușită*
    * **Given** că am un cont existent
    * **When** introduc datele de autentificare corecte
    * **Then** sunt autentificat în aplicație
  * *Scenario: Autentificare eșuată*
    * **Given** că am un cont existent
    * **When** introduc o parolă incorectă
    * **Then** sistemul afișează un mesaj de eroare
* **US 1.3 – Deconectarea utilizatorului**
  * *Scenario: Deconectare*
    * **Given** că sunt autentificat în aplicație
    * **When** aleg opțiunea de deconectare
    * **Then** sesiunea mea este încheiată

### Epic 2 – Gestionarea evenimentelor
* **US 2.1 – Crearea unui eveniment**
  * *Scenario: Crearea unui eveniment*
    * **Given** că sunt autentificat ca organizator aprobat
    * **When** completez informațiile necesare și salvez evenimentul
    * **Then** evenimentul este creat în sistem
* **US 2.2 – Publicarea unui eveniment**
  * *Scenario: Publicarea unui eveniment*
    * **Given** că am creat un eveniment cu informațiile necesare
    * **When** public evenimentul
    * **Then** evenimentul apare în lista publică
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
* **Actori:**
  - *Participant/Client;*
  - *Organizatorul evenimentului;*
  - *Administratorul platformei;*
* **Sistemul:** Elide Events, format din aplicația web React și un API REST Spring Boot care folosește o bază de date PostgreSQL.
* **Sisteme externe:**
  - *Procesator de plăți (Stripe în Test Mode):* Procesează plățile cu cardul și confirmă tranzacțiile.
  - *Serviciu de email (SendGrid):* Trimite confirmări de cont, biletele cu cod QR și notificările de anulare.
  - *Google Maps Platform:* Utilizat pentru serviciul de geocodare (Geocoding API), transformând adresele introduse de organizatori în coordonate geografice (latitudine și longitudine).
  - *OpenStreetMap (cu MapLibre GL JS):* Utilizat pentru furnizarea datelor și a stilului de hartă interactivă în aplicația web, permițând afișarea vizuală a locației evenimentului și a markerelor.

Participantul își creează un cont (confirmat prin email), se autentifică, caută evenimente și vede detaliile unui eveniment, inclusiv harta locației. La rezervarea biletelor, locurile sunt blocate temporar. Dacă evenimentul are sală configurată, participantul își alege locurile direct pe harta sălii (*Selectare loc pe harta sălii*, `«extend»`). Dacă plata nu se finalizează în intervalul de 10 minute, locurile sunt eliberate automat (*Eliberare locuri la expirarea rezervării*, `«extend»`). Plata biletelor se face prin Stripe (*Procesare plată prin card*, `«include»`), iar după confirmare sistemul emite biletele cu cod QR și le trimite pe email (*Emitere bilete cu cod QR*, `«include»`). Dacă plata este respinsă, se execută *Gestionare plată eșuată* (`«extend»`), iar biletele nu se emit.

Organizatorul creează și editează evenimente. La salvare, adresa introdusă este transformată în coordonate geografice (*Geocodare adresă*, `«include»`). Tot el gestionează categoriile de bilete, configurează harta sălii și publică evenimentul. Publicarea este permisă doar unui organizator aprobat (*Verificare organizator aprobat*, `«include»`). Funcțiile avansate sunt disponibile pe bază de abonament, iar activarea abonamentului Pro se plătește tot prin Stripe. La intrare, organizatorul validează biletele prin scanarea codului QR, iar un bilet invalid sau deja utilizat este refuzat (*Refuzare bilet invalid sau utilizat*, `«extend»`). De asemenea, vizualizează rapoarte de vânzări și statistici de performanță.

Administratorul platformei aprobă organizatorii, gestionează utilizatorii și permisiunile conturilor și monitorizează funcționarea sistemului.

Relațiile `«include»` marchează un comportament obligatoriu, care face parte din cazul de bază. Relațiile `«extend»` marchează un comportament condiționat, care apare doar într-o situație anume. Cazul de bază rămâne complet și fără extensie.

*(Diagrama completă este disponibilă în documentația grafică atașată proiectului).*

---

## 9. Diagrama C4 – Nivel 1: Context & Nivel 2: Containere

### Nivel 1: Context
Diagrama de context prezintă Elide Events ca un singur sistem: o platformă SaaS de ticketing și management de evenimente, cu utilizatorii și serviciile externe cu care interacționează.

Sistemul are trei tipuri de utilizatori. Participantul caută evenimente, rezervă și cumpără bilete și primește biletul cu cod QR. Organizatorul creează și administrează evenimente, categorii de bilete și hărți de săli și urmărește vânzările, pe planul Gratuit sau Pro. Administratorul platformei aprobă organizatorii, gestionează utilizatorii și permisiunile și monitorizează sistemul. Toți folosesc aplicația prin HTTPS. Platforma depinde de patru servicii externe. Diagrama nu arată structura internă a sistemului. Aceasta este detaliată în diagrama de containere.

### Nivel 2: Containere
Diagrama de containere împarte aplicația în trei părți care rulează separat, plus serviciile externe:
1. **Aplicația web (SPA, React):** Este interfața folosită de toți cei trei actori. Conține lista de evenimente, selectorul de locuri (Seat Map), pagina de checkout, harta interactivă și panourile pentru organizator și administrator. Comunică cu API-ul prin apeluri JSON/HTTPS (REST) și încarcă stilul și datele hărții de la OpenStreetMap prin MapLibre GL JS.
2. **API-ul REST (Java, Spring Boot):** Conține logica de business, organizată pe straturi (controller, service, repository, cu acces la date prin JPA). Se ocupă de autentificare și roluri, evenimente, rezervări cu expirare la 10 minute, emiterea biletelor cu cod QR, validarea codurilor QR și rapoarte. Contractul REST este documentat cu OpenAPI. API-ul creează sesiunea de plată Stripe Checkout în test mode, trimite emailuri prin SendGrid și apelează Geocoding API pentru adrese.
3. **Baza de date (PostgreSQL):** Păstrează conturile, evenimentele, sălile și locurile, categoriile de bilete, rezervările, biletele, plățile și abonamentele. Tranzacțiile ACID garantează că același loc nu poate fi vândut de două ori. API-ul citește și scrie în ea prin JDBC.

---

## 10. Diagrama UML (Diagrama de clase a domeniului)
Diagrama de clase descrie modelul de domeniu al aplicației, împărțit în trei zone: conturi și abonamente, evenimente, rezervări și bilete, locații și hărți. Atributele marcate cu „/” sunt derivate, iar enumerările sunt explicate în legendă.

- **Conturi și abonamente:** `Utilizator` este o clasă abstractă, cu atributele comune tuturor conturilor (nume, email, parolă criptată, confirmarea emailului, data înregistrării). Din ea derivă `Administrator`, `Organizator` și `Participant`. Organizatorul are în plus denumire, cod fiscal, date de contact și un statut de aprobare (în așteptare, aprobat sau respins). Un organizator deține zero sau mai multe abonamente, iar fiecare abonament este încheiat conform unui plan (Gratuit sau Pro). Planul stabilește prețul lunar, limita de evenimente și de capacitate și dacă sunt permise harta de locuri, rapoartele avansate și validarea QR.
- **Evenimente, rezervări și bilete:** Un organizator organizează mai multe evenimente. Evenimentul are tip, perioadă de desfășurare, stare (propus, publicat, anulat, încheiat) și vizibilitate (public sau privat) și poate fi publicat, anulat sau încheiat. El conține categorii de bilete, cu preț, capacitate și perioadă de vânzare, iar numărul de locuri vândute, rezervate și disponibile se calculează. Un participant efectuează mai multe rezervări. O rezervare are un număr de bilete, un total și un termen de expirare și poate fi confirmată sau poate expira. Ea se achită prin una sau mai multe plăți Stripe și emite biletele, adică este în relație de compoziție cu ele. Fiecare bilet are un cod QR unic, o stare (emis, utilizat, anulat) și poate fi validat.
- **Locații și hărți:** Un eveniment se desfășoară la o locație (cu adresă și coordonate) și poate folosi o sală, formată din locuri cu rând, număr și poziție pe hartă. Clasa `LocEveniment` descrie un loc pentru un anumit eveniment și îi ține starea (liber, rezervat temporar, ocupat). Astfel, același loc fizic poate fi folosit la mai multe evenimente, iar relația mulți-la-mulți dintre Eveniment și Loc este rezolvată prin `LocEveniment`. O rezervare blochează locuri, iar un bilet poate fi legat de un anumit loc.

---

## 11. Diagrama SQL (Model relațional, PostgreSQL)
Diagrama bazei de date traduce modelul de clase în tabele PostgreSQL. Cheile primare sunt marcate PK, cheile străine FK, iar cele unice UK.

- **Moștenirea conturilor:** Ierarhia `Utilizator` – `Administrator`, `Organizator`, `Participant` este implementată prin strategia joined table. Tabelul `utilizator` conține coloanele comune și coloana `tip_utilizator`, iar fiecare subclasă are propriul tabel, a cărui cheie primară este totodată cheie străină către utilizator. Câmpurile specifice (de exemplu `cod_fiscal` și `statut_aprobare` la organizator) stau în tabelul subclasei.
- **Tabelele, pe zone:**
  - *Conturi și abonamente:* `utilizator`, `administrator`, `organizator`, `participant`, `abonament_organizator`, `plan_abonament`.
  - *Evenimente, rezervări și bilete:* `eveniment`, `categorie_bilet`, `rezervare`, `plata`, `bilet`, `loc_eveniment`.
  - *Locații și hărți:* `locatie`, `sala`, `loc`.
- **Constrângeri importante:** Au valori unice emailul utilizatorului, codul fiscal, codul QR al biletului, referința plății Stripe și tipul de plan. Tabelul `loc_eveniment` leagă un eveniment de un loc fizic și ține starea locului. Biletul are cheie străină unică către `loc_eveniment`, pentru ca același loc să nu poată fi vândut de două ori. Sumele de bani sunt `NUMERIC(10,2)`, momentele de timp sunt `TIMESTAMP`, iar coordonatele sunt `DOUBLE PRECISION`. Coloanele de stare sunt `VARCHAR` cu constrângere `CHECK`, cu aceleași valori ca enumerările din diagrama de clase.
