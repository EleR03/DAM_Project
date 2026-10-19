## Diagrame Sprint 1
## 1. Diagrama Use Case
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

## 2. Diagrama C4 – Nivel 1: Context 

### Nivel 1: Context
Diagrama de context prezintă Elide Events ca un singur sistem: o platformă SaaS de ticketing și management de evenimente, cu utilizatorii și serviciile externe cu care interacționează.

Sistemul are trei tipuri de utilizatori. Participantul caută evenimente, rezervă și cumpără bilete și primește biletul cu cod QR. Organizatorul creează și administrează evenimente, categorii de bilete și hărți de săli și urmărește vânzările, pe planul Gratuit sau Pro. Administratorul platformei aprobă organizatorii, gestionează utilizatorii și permisiunile și monitorizează sistemul. Toți folosesc aplicația prin HTTPS. Platforma depinde de patru servicii externe. Diagrama nu arată structura internă a sistemului. Aceasta este detaliată în diagrama de containere.

## 3. Diagrama C4 - Nivel 2: Containere
Diagrama de containere împarte aplicația în trei părți care rulează separat, plus serviciile externe:
1. **Aplicația web (SPA, React):** Este interfața folosită de toți cei trei actori. Conține lista de evenimente, selectorul de locuri (Seat Map), pagina de checkout, harta interactivă și panourile pentru organizator și administrator. Comunică cu API-ul prin apeluri JSON/HTTPS (REST) și încarcă stilul și datele hărții de la OpenStreetMap prin MapLibre GL JS.
2. **API-ul REST (Java, Spring Boot):** Conține logica de business, organizată pe straturi (controller, service, repository, cu acces la date prin JPA). Se ocupă de autentificare și roluri, evenimente, rezervări cu expirare la 10 minute, emiterea biletelor cu cod QR, validarea codurilor QR și rapoarte. Contractul REST este documentat cu OpenAPI. API-ul creează sesiunea de plată Stripe Checkout în test mode, trimite emailuri prin SendGrid și apelează Geocoding API pentru adrese.
3. **Baza de date (PostgreSQL):** Păstrează conturile, evenimentele, sălile și locurile, categoriile de bilete, rezervările, biletele, plățile și abonamentele. Tranzacțiile ACID garantează că același loc nu poate fi vândut de două ori. API-ul citește și scrie în ea prin JDBC.

---

## 4. Diagrama UML (Diagrama de clase a domeniului)
Diagrama de clase descrie modelul de domeniu al aplicației, împărțit în trei zone: conturi și abonamente, evenimente, rezervări și bilete, locații și hărți. Atributele marcate cu „/” sunt derivate, iar enumerările sunt explicate în legendă.

- **Conturi și abonamente:** `Utilizator` este o clasă abstractă, cu atributele comune tuturor conturilor (nume, email, parolă criptată, confirmarea emailului, data înregistrării). Din ea derivă `Administrator`, `Organizator` și `Participant`. Organizatorul are în plus denumire, cod fiscal, date de contact și un statut de aprobare (în așteptare, aprobat sau respins). Un organizator deține zero sau mai multe abonamente, iar fiecare abonament este încheiat conform unui plan (Gratuit sau Pro). Planul stabilește prețul lunar, limita de evenimente și de capacitate și dacă sunt permise harta de locuri, rapoartele avansate și validarea QR.
- **Evenimente, rezervări și bilete:** Un organizator organizează mai multe evenimente. Evenimentul are tip, perioadă de desfășurare, stare (propus, publicat, anulat, încheiat) și vizibilitate (public sau privat) și poate fi publicat, anulat sau încheiat. El conține categorii de bilete, cu preț, capacitate și perioadă de vânzare, iar numărul de locuri vândute, rezervate și disponibile se calculează. Un participant efectuează mai multe rezervări. O rezervare are un număr de bilete, un total și un termen de expirare și poate fi confirmată sau poate expira. Ea se achită prin una sau mai multe plăți Stripe și emite biletele, adică este în relație de compoziție cu ele. Fiecare bilet are un cod QR unic, o stare (emis, utilizat, anulat) și poate fi validat.
- **Locații și hărți:** Un eveniment se desfășoară la o locație (cu adresă și coordonate) și poate folosi o sală, formată din locuri cu rând, număr și poziție pe hartă. Clasa `LocEveniment` descrie un loc pentru un anumit eveniment și îi ține starea (liber, rezervat temporar, ocupat). Astfel, același loc fizic poate fi folosit la mai multe evenimente, iar relația mulți-la-mulți dintre Eveniment și Loc este rezolvată prin `LocEveniment`. O rezervare blochează locuri, iar un bilet poate fi legat de un anumit loc.

---

## 5. Diagrama SQL (Model relațional, PostgreSQL)
Diagrama bazei de date traduce modelul de clase în tabele PostgreSQL. Cheile primare sunt marcate PK, cheile străine FK, iar cele unice UK.

- **Moștenirea conturilor:** Ierarhia `Utilizator` – `Administrator`, `Organizator`, `Participant` este implementată prin strategia joined table. Tabelul `utilizator` conține coloanele comune și coloana `tip_utilizator`, iar fiecare subclasă are propriul tabel, a cărui cheie primară este totodată cheie străină către utilizator. Câmpurile specifice (de exemplu `cod_fiscal` și `statut_aprobare` la organizator) stau în tabelul subclasei.
- **Tabelele, pe zone:**
  - *Conturi și abonamente:* `utilizator`, `administrator`, `organizator`, `participant`, `abonament_organizator`, `plan_abonament`.
  - *Evenimente, rezervări și bilete:* `eveniment`, `categorie_bilet`, `rezervare`, `plata`, `bilet`, `loc_eveniment`.
  - *Locații și hărți:* `locatie`, `sala`, `loc`.
- **Constrângeri importante:** Au valori unice emailul utilizatorului, codul fiscal, codul QR al biletului, referința plății Stripe și tipul de plan. Tabelul `loc_eveniment` leagă un eveniment de un loc fizic și ține starea locului. Biletul are cheie străină unică către `loc_eveniment`, pentru ca același loc să nu poată fi vândut de două ori. Sumele de bani sunt `NUMERIC(10,2)`, momentele de timp sunt `TIMESTAMP`, iar coordonatele sunt `DOUBLE PRECISION`. Coloanele de stare sunt `VARCHAR` cu constrângere `CHECK`, cu aceleași valori ca enumerările din diagrama de clase.
