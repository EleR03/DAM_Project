# DAM_Project
Aplicatie pentru organizarea si centralizarea evenimentelor
Elide Events 

Tema aplicației și funcționalitățile principale 

Elide Events funcționează ca o platformă SaaS de ticketing și management de evenimente. Organizatorii pot crea și administra evenimente, stabili categorii de bilete și urmări vânzările, iar participanții pot căuta evenimente, rezerva și cumpăra bilete online.  Pentru organizatori, accesul la funcționalitățile avansate (publicare nelimitată de evenimente, configurarea sălilor cu locuri numerotate și statistici detaliate) este oferit pe bază de abonament (subscripție). Încasarea biletelor este procesată securizat și centralizat prin platformă (integrare Stripe), sistemul generând automat biletele cu cod QR unic și eliberând locurile în cazul nefinalizării plății. Administratorii platformei supraveghează activitatea utilizatorilor și validează organizatorii. 

 

Problema pe care o rezolvă aplicația este lipsa unei platforme centralizate pentru gestionarea evenimentelor și monitorizarea biletelor vândute. În multe situații, organizatorii utilizează fișiere Excel sau evidențe manuale, ceea ce poate genera erori privind disponibilitatea biletelor, dificultăți de raportare și procese lente de validare la intrarea în evenimente. 

Funcționalități principale: 

gestionarea conturilor de utilizator (participant/client, organizatorul evenimentului, administratorul platformei); 

publicarea și administrarea evenimentelor (operații CRUD, funcție de vizibilitate a evenimentului public/privat); 

configurarea categoriilor de bilete și emiterea/generarea acestora; 

rezervarea și cumpărarea biletelor pentru participanți; 

rezervarea poate bloca temporar un loc, iar cumpărarea confirmă biletul după procesarea cu succes a plății. Dacă plata eșuează sau rezervarea expiră, locul trebuie eliberat. 

validarea biletelor prin scanarea codului QR; 

localizarea evenimentelor pe hartă: Integrare cu servicii de hărți (ex. Google Maps API) pentru afișarea locației exacte a evenimentului și obținerea indicațiilor de orientare. 

selectarea interactivă a locurilor (Seat Map / Hartă interactivă a sălii): Posibilitatea participanților de a vizualiza sala (rânduri, locuri libere/ocupate/rezervate temporar în stil cinema/teatru) și de a selecta direct locul dorit. 

rapoarte de vânzări pentru organizatori și rapoarte ale platformei pentru administrator; 

generarea statisticilor de performanță. 

 

Echipă și roluri 

Rol 

Membru 

Product Owner și Business Analyst 

 

Tech Lead și Architect 

 

Developer și QA Tester 

 

 

Backlog inițial: Epics 

Epic 1 – Autentificare și gestionarea conturilor - Utilizatorii își pot crea conturi și se pot autentifica în aplicație. Sistemul gestionează rolurile de client, organizator și administrator. 

Epic 2 – Gestionarea evenimentelor - Organizatorii aprobați pot crea și publica evenimente, iar clienții pot căuta evenimentele publicate. 

Epic 3 – Rezervări și bilete - Clienții pot rezerva bilete, iar sistemul gestionează disponibilitatea acestora și expirarea rezervărilor. 

Epic 4 – Plăți - Clienții pot plăti rezervările, iar sistemul confirmă plățile și permite emiterea biletelor. 

Epic 5 – Validarea biletelor - Sistemul verifică validitatea biletelor prin codul QR și identifică biletele deja utilizate. 

Epic 6 – Raportare și administrare - Organizatorii pot consulta rapoartele de vânzări, iar administratorul poate aproba conturile organizatorilor. 

 

User Stories (Gherkin) 

US 1.1 – Înregistrarea unui client 

Scenario: Înregistrare reușită 

Given că sunt un vizitator fără cont 
When completez datele necesare și trimit formularul 
Then contul meu este creat 
And primesc un email de confirmare 

 

US 1.2 – Autentificarea utilizatorului 

Scenario: Autentificare reușită 

Given că am un cont existent 
When introduc datele de autentificare corecte 
Then sunt autentificat în aplicație 

 

Scenario: Autentificare eșuată 

Given că am un cont existent 
When introduc o parolă incorectă 
Then sistemul afișează un mesaj de eroare 

 

US 1.3 – Scenario: Deconectare 

Given că sunt autentificat în aplicație 
When aleg opțiunea de deconectare 
Then sesiunea mea este încheiată 

 

US 2.1 – Scenario: Crearea unui eveniment 

Given că sunt autentificat ca organizator aprobat 
When completez informațiile necesare și salvez evenimentul 
Then evenimentul este creat în sistem 

 

US 2.2 –Scenario: Publicarea unui eveniment 

Given că am creat un eveniment cu informațiile necesare 
When public evenimentul 
Then evenimentul apare în lista publică 

 

US 2.3 – Scenario: Căutarea evenimentelor 

Given că există evenimente publicate 
When caut evenimente după oraș sau dată 
Then sunt afișate evenimentele care corespund criteriilor 

 

US 2.4 – Scenario: Afișare hartă locație 

Given că accesez detaliile unui eveniment publicat 

When vizualizez secțiunea de adresă/locație 

Then sistemul încarcă harta interactivă cu marcajul poziției evenimentului 

 

US 3.1 –Scenario: Rezervarea biletelor disponibile 

Given că sunt autentificat și există bilete disponibile 
When rezerv două bilete VIP 
Then rezervarea este creată pentru 10 minute 

 

US 3.2 –Scenario: Rezervare expirată 

Given că am o rezervare neplătită 
When expiră perioada de 10 minute 
Then rezervarea este anulată 
And biletele redevin disponibile 

 

US 3.3 - Scenario: Bilete epuizate 

Given că toate biletele din categoria aleasă sunt indisponibile 
When încerc să rezerv un bilet 
Then sistemul afișează un mesaj că biletele sunt epuizate 

 

US 3.4 – Scenario: Selectarea locului pe harta interactivă a sălii 

Given că evenimentul are alocare de locuri numerotate și există locuri disponibile 

When selectez un scaun marcat ca fiind liber de pe planul sălii 

Then scaunul își schimbă starea în „selectat” 

And locul este rezervat temporar pentru 10 minute 

 

Scenario: Încercare selectare loc deja ocupat/rezervat 

Given că un loc de pe hartă este deja rezervat sau cumpărat 

When încerc să dau click pe acel loc 

Then sistemul nu îmi permite selectarea lui și afișează că este indisponibil 

 

 

US 4.1 – Plata unei rezervări 

Scenario: Plată reușită 

Given că am o rezervare activă 
When procesatorul de plăți confirmă plata 
Then comanda este marcată ca plătită 
And biletele sunt emise 
And primesc confirmarea prin email 

 

US 4.2 – Scenario: Plată eșuată 

Given că am o rezervare activă 
When procesatorul de plăți respinge plata 
Then sistemul afișează un mesaj de eroare 
And biletele nu sunt emise 

 

US 5.1 – Scenario: Bilet valid 

Given că există un bilet valid și neutilizat 
When codul QR este verificat 
Then sistemul confirmă că biletul este valid 

 

US 5.2 – Scenario: Bilet invalid 

Given că un cod QR nu corespunde unui bilet valid 
When codul QR este verificat 
Then sistemul afișează un mesaj că biletul este invalid 

 

US 5.3 – Scenario: Bilet deja utilizat 

Given că biletul a fost deja utilizat 
When codul QR este verificat din nou 
Then sistemul informează că biletul a fost deja utilizat 

 

US 6.1 – Scenario: Vizualizarea raportului de vânzări 

Given că sunt autentificat ca organizator al evenimentului 
When accesez raportul evenimentului 
Then sistemul afișează biletele vândute pe categorie și venitul confirmat 

 

US 6.2 – Aprobarea unui organizator - Scenario: Aprobare reușită 

Given că există un organizator care așteaptă aprobarea 
When aprob contul acestuia 
Then statutul organizatorului devine "Aprobat" 

 

 

 

 

 

ADR 001 – Alegerea stack-ului tehnologic 

Status: acceptat 

Context: Aplicația are logică de business care trebuie respectată strict (stoc limitat de bilete, expirarea rezervărilor, validare unică la intrare) și trebuie să fie organizată pe straturi separate: prezentare, logică de business, acces la date. Avem nevoie de un stack care permite tranzacții sigure, teste automate și CI/CD. 

Opțiuni analizate: 

Java + Spring Boot + PostgreSQL + React 

.NET Core + SQL Server + Angular 

Node.js + MongoDB + Vue.js 

Criteriu 

Spring Boot + PostgreSQL + React 

.NET + SQL Server + Angular 

Node + MongoDB + Vue 

Arhitectură pe straturi 

Foarte bună (controller / service / repository) 

Foarte bună 

Depinde de disciplina echipei 

Tranzacții și consistență 

Excelentă (ACID) 

Excelentă (ACID) 

Mai slabă pentru stocuri și plăți 

Cost licențe 

Zero 

Licențe SQL Server 

Zero 

Cunoștințe în echipă 

Bune 

Parțiale 

Parțiale 

Testare și CI/CD 

JUnit, Testcontainers, GitHub Actions 

xUnit, Azure DevOps 

Jest, GitHub Actions 

Decizie: Se alege Java + Spring Boot + PostgreSQL + React. 

Motive: 

Vânzarea de bilete cere tranzacții și blocări corecte, ca două persoane să nu cumpere același loc. O bază relațională precum PostgreSQL rezolvă asta mult mai sigur decât MongoDB. 

Spring Boot impune natural separarea pe straturi, ceea ce se potrivește cu cerințele proiectului. 

Tehnologiile alese sunt open-source, eliminând necesitatea costurilor legate de licențiere. 

React permite o interfață modernă, cu componente reutilizabile (listă evenimente, selector de locuri, pagină de checkout). 

Testele automate (JUnit, Testcontainers pentru PostgreSQL) și pipeline-ul CI/CD se integrează simplu. 

Consecințe: 

Pozitive: curbă de învățare mică, documentație bogată, comunitate mare, deploy ușor prin Docker sau Vercel. 

Negative: configurarea inițială a Spring Boot este mai minuțioasă decât la Node, deoarece frontend-ul și backend-ul se dezvoltă în două proiecte separate, cu un contract REST de menținut (documentat cu OpenAPI). 

GitHub 

Repository-ul GitHub se poate găsi accesând: 

REPOSITORY GITHUB 

https://github.com/EleR03/DAM_Project.git 

Tool management taskuri 

Am ales platforma Trello pentru organizarea și monitorizarea activităților echipei, deoarece oferă o interfață vizuală simplă, bazată pe panouri, liste și carduri. 

Vom utiliza un board dedicat proiectului Elide Events, organizat în următoarele liste: 

Product Backlog – toate Epics și User Stories identificate. 

To Do – sarcinile selectate pentru Sprintul curent. 

In Progress – sarcinile aflate în implementare. 

Code Review / Testing – sarcinile care necesită verificare. 

Done – sarcinile finalizate și acceptate. 

Fiecare card va conține descrierea sarcinii, persoana responsabilă, prioritatea, criteriile de acceptare și, dacă este cazul, estimarea în Story Points. 

Trello va facilita distribuirea responsabilităților, urmărirea progresului și identificarea sarcinilor blocate. 

 

Diagrama Use Case 

Actori: 

Participant/Client; 

Organizatorul evenimentului; 

Administratorul platformei; 

Sistemul: Elide Events, format din aplicația web React și un API REST Spring Boot care folosește o bază de date PostgreSQL. 

Sisteme externe: 

Procesator de plăți (de exemplu Stripe) : procesează plățile cu cardul și confirmă tranzacțiile. 

Serviciu de email (de exemplu SendGrid): trimite confirmări de cont, biletele cu cod QR și notificările de anulare. 

Google Maps: afișează locația evenimentului pe hartă și transformă adresa în coordonate. 

 

Use Cases: 

Participant: 

Înregistrare 

Autentificare 

Vizualizare evenimente  

Rezervare bilete 

Achiziție bilete 

Vizualizare bilete 

Organizator: 

Operații CRUD pentru eveniment(e) 

Gestionare și emitere bilete 

Vizualizare rapoarte și generare de statistici de performanță 

Administrator: 

Gestionare utilizatori și permisiuni conturi 

Aprobare statut de organizator 

Monitorizarea sistemului 

 

Diagrama C4 – Context 

 

 Diagrama UML (diagrama de clase a domeniului) 

 

 Diagrama SQL (model relațional, PostgreSQL) 

 

 README.md (tema documentată) 

 
