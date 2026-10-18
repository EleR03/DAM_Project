# ADR 001 – Alegerea stack-ului tehnologic

**Status:** acceptat

## Context
Aplicația are logică de business care trebuie respectată strict (stoc
limitat de bilete, expirarea rezervărilor, validare unică la intrare) și
trebuie să fie organizată pe straturi separate: prezentare, logică de
business, acces la date. Avem nevoie de un stack care permite tranzacții
sigure, teste automate și CI/CD.

## Opțiuni analizate
1. Java + Spring Boot + PostgreSQL + React
2. .NET Core + SQL Server + Angular
3. Node.js + MongoDB + Vue.js

| Criteriu | Spring Boot + PostgreSQL + React | .NET + SQL Server + Angular | Node + MongoDB + Vue |
|----------|----------------------------------|-----------------------------|----------------------|
| Arhitectură pe straturi | Foarte bună (controller / service / repository) | Foarte bună | Depinde de disciplina echipei |
| Tranzacții și consistență | Excelentă (ACID) | Excelentă (ACID) | Mai slabă pentru stocuri și plăți |
| Cost licențe | Zero | Licențe SQL Server | Zero |
| Cunoștințe în echipă | Bune | Parțiale | Parțiale |
| Testare și CI/CD | JUnit, Testcontainers, GitHub Actions | xUnit, Azure DevOps | Jest, GitHub Actions |

## Decizie
Se alege **Java + Spring Boot + PostgreSQL + React**.

## Motive
- Vânzarea de bilete cere tranzacții și blocări corecte, ca două persoane
  să nu cumpere același loc. O bază relațională precum PostgreSQL rezolvă
  asta mult mai sigur decât MongoDB.
- Spring Boot impune natural separarea pe straturi, ceea ce se potrivește
  cu cerințele proiectului.
- Tehnologiile alese sunt open-source, eliminând costurile de licențiere.
- React permite o interfață modernă, cu componente reutilizabile (listă
  evenimente, selector de locuri, pagină de checkout).
- Testele automate (JUnit, Testcontainers pentru PostgreSQL) și pipeline-ul
  CI/CD se integrează simplu.

## Consecințe
**Pozitive:** curbă de învățare mică, documentație bogată, comunitate
mare, deploy ușor prin Docker sau Vercel.

**Negative:** configurarea inițială a Spring Boot este mai minuțioasă
decât la Node, deoarece frontend-ul și backend-ul se dezvoltă în două
proiecte separate, cu un contract REST de menținut (documentat cu OpenAPI).
