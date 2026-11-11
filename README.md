# 📚 Library Management System - REST API

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen.svg)
![Spring Security](https://img.shields.io/badge/Spring%20Security-RBAC-blue.svg)
![Tests](https://img.shields.io/badge/tests-111%20passing-success.svg)
![Maven](https://img.shields.io/badge/Maven-Build-red.svg)

A RESTful API for the daily work of a library: catalog, members, borrowing and returning books, reservations,
overdue loans, fines, reports and administration. The system was developed with the software engineering process:
requirements analysis, UML design, implementation and testing. The design documents are in the folder [`docs`](docs).

## 🚀 Tech Stack
* **Core:** Java 21, Spring Boot 4
* **Data access:** Spring Data JPA, Hibernate ORM
* **Security:** Spring Security (HTTP Basic, BCrypt, role based access)
* **Database:** H2 in memory (default, no setup) or MySQL (profile `mysql`)
* **API documentation:** springdoc OpenAPI (Swagger UI)
* **Testing:** JUnit 5, Mockito, Spring MockMvc, JaCoCo
* **Utilities:** Lombok, Maven

## 🏗️ System Architecture
Layered architecture, as in the class diagram:

| Package | Responsibility |
| :--- | :--- |
| `controller` | REST endpoints, role rules (`@PreAuthorize`), conversion to DTOs |
| `service` | Business logic: borrowing rules, fines, reservations, notifications, reports |
| `repository` | Spring Data JPA repositories |
| `entity` | Domain model: `User` (abstract) → `Member`, `Librarian` → `Administrator`, `Book`, `BorrowingRecord`, `Fine`, `Reservation`, `LibraryRules` |
| `dto` | Request and response objects with validation |
| `exception` | Business exceptions and one global error handler (JSON error body with a clear code) |
| `security` / `config` | Security rules, password encoder, OpenAPI, demo data |

## ✨ Features (with requirement IDs)
| Area | Features |
| :--- | :--- |
| Account and access | Registration (FR-001), login (FR-002), roles Member / Librarian / Administrator (FR-003) |
| Catalog | Add, update, remove, search and view books (FR-004 to FR-008). Removed books stay in the history |
| Members | Add, update, view, activate and deactivate members (FR-009 to FR-012) |
| Circulation | Borrow with checks for active member, loan limit and available copies (FR-013 to FR-016), return and automatic stock update (FR-017, FR-018), renew once (FR-019), active loans and history (FR-020, FR-021), overdue list (FR-022) |
| Reservations | Reserve a book that has no copy (FR-023), cancel (FR-024), e-mail to the first waiting member when the book is returned (FR-025), overdue reminders (FR-026) |
| Fines | Fine = late days × daily rate, created automatically on late return (FR-027), payment record (FR-028) |
| Administration | Staff accounts (FR-029), library rules: loan period, loan limit, renewals, fine rate (FR-030), reports (FR-031) |
| Non-functional | BCrypt hashes (NFR-003), login for all functions (NFR-004), audit log `AUDIT` (NFR-005), transactions and optimistic locking so stock never gets negative (NFR-006), clear error messages (NFR-010), layered design (NFR-011), tests with coverage report (NFR-012), H2 / MySQL (NFR-013) |

> E-mails are simulated: the `NotificationService` writes them to the log. To use a real mail server, only
> `NotificationService.sendEmail` has to be changed.

## 🔌 API Endpoints
All endpoints need HTTP Basic login (e-mail and password) except registration and the API documentation.

| Method | Endpoint | Role | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | public | Create a member account |
| `GET` | `/api/auth/me` | any | Log in check, returns profile and role |
| `GET` | `/api/books?keyword=` | any | Search by title, author, ISBN or category |
| `GET` | `/api/books/{id}` | any | Book details |
| `POST` `PUT` `DELETE` | `/api/books`, `/api/books/{id}` | Librarian | Add, update, remove a book |
| `GET` `POST` `PUT` | `/api/members`, `/api/members/{id}` | Librarian | Search, view (with loans and unpaid fines), add, update members |
| `PATCH` | `/api/members/{id}/activate`, `/deactivate` | Librarian | Activate or deactivate a member |
| `POST` | `/api/library/borrow?userId={id}&bookId={id}` | Librarian | Borrow a book (due date from the library rules) |
| `POST` | `/api/library/return/{recordId}` | Librarian | Return a book, creates a fine if late |
| `POST` | `/api/library/renew/{recordId}` | Member | Renew own loan |
| `GET` | `/api/library/user/{userId}/active`, `/history` | Librarian or the member himself | Active loans, borrowing history |
| `GET` | `/api/library/overdue` | Librarian | All overdue loans |
| `POST` | `/api/library/overdue/remind` | Librarian | Send overdue reminder e-mails |
| `POST` `DELETE` `GET` | `/api/reservations?bookId=`, `/{id}`, `/my` | Member | Reserve, cancel, list own reservations |
| `GET` `POST` | `/api/fines/unpaid`, `/api/fines/{id}/pay` | Librarian | Unpaid fines, record a payment |
| `GET` | `/api/fines/my` | Member | Own fines |
| `GET` | `/api/reports/most-borrowed`, `/overdue`, `/member-activity` | Librarian | Reports |
| `GET` `POST` `PATCH` | `/api/admin/staff`, `/{id}/activate`, `/deactivate` | Administrator | Staff accounts |
| `GET` `PUT` | `/api/admin/rules` | Administrator | Library rules |

Errors have the same JSON body: `{"timestamp": "...", "status": 409, "code": "BookNotAvailable", "message": "..."}`.

## 🛠️ How to Run Locally

1. **Clone the repository:**
   ```bash
   git clone https://github.com/SabriGR/library-management-system.git
   cd library-management-system
   ```
2. **Run the application** (Java 21 is needed; the H2 database needs no setup):
   ```bash
   ./mvnw spring-boot:run
   ```
   The API is available at `http://localhost:8080`, the Swagger UI at `http://localhost:8080/swagger-ui.html`
   (use the *Authorize* button with an e-mail and a password).
3. **MySQL (optional):** set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` as environment variables and start with
   `./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql`.

### Demo accounts
The `DataSeeder` creates demo data on an empty database (books, loans, an overdue loan, a fine, a reservation).
These accounts are for demonstration only:

| Role | E-mail | Password |
| :--- | :--- | :--- |
| Administrator | `admin@library.com` | `Admin123!` |
| Librarian | `librarian@library.com` | `Librarian123!` |
| Member | `sabri@library.com`, `bahri@library.com`, `oguzhan@library.com` | `Member123!` |

Example:
```bash
curl -u librarian@library.com:Librarian123! "http://localhost:8080/api/books?keyword=clean"
curl -u librarian@library.com:Librarian123! -X POST "http://localhost:8080/api/library/borrow?userId=3&bookId=2"
```

## 🧪 Testing
```bash
./mvnw test
```
111 tests run, all pass. The JaCoCo coverage report is created in `target/site/jacoco/index.html`. Coverage: service layer 87.6%, `LibraryService` 100%, whole project 90%.

| Test class | Technique |
| :--- | :--- |
| `LibraryServiceBlackBoxTest` | **Black box:** equivalence partitioning and boundary value analysis of borrow, return (fine) and renew, derived only from the requirements |
| `LibraryServiceWhiteBoxTest` | **White box:** all decision nodes and basis paths of `borrowBook` (V(G) = 6), `returnBook` (V(G) = 4) and `renewLoan` (V(G) = 7). Statement and branch coverage of `LibraryService` is 100% |
| `DomainModelTest` | Unit tests of the entities: stock rules, overdue boundary, fine calculation, reservation life cycle |
| `SupportServicesTest` | Business rules of the book, member, staff and reservation services |
| `LibraryApiIntegrationTest` | End-to-end tests with real security: roles, registration, catalog, borrow and return, loan limit, reservation, overdue and fines, reports, administration |
| `PerformanceAndConcurrencyTest` | **Non-functional:** search in 10,000 books (NFR-001, measured 139 ms), 50 users at the same time (NFR-002), 50 members borrow the last copy at the same time: exactly one succeeds and the stock never gets negative (NFR-006) |

## 📐 Design Documents
The folder [`docs`](docs) contains the project documents:

| Folder | Content |
| :--- | :--- |
| [`docs/documents`](docs/documents) | Software Requirements Specification (SRS) and the project report (Word) |
| [`docs/requirements`](docs/requirements) | Requirements database (Excel) with implementation and verification of every requirement |
| [`docs/uml`](docs/uml) | Use case, class (domain and services), sequence, communication, state and activity diagrams, the architecture figure and the control flow graphs used for white box testing. The sources are in `docs/uml/source` |

The use case IDs (UC-xx) and requirement IDs (FR-xxx, NFR-xxx) in this README, in the code comments and in the tests refer to these documents.

## ⚠️ Known Limitations
* No graphical user interface (REST API and Swagger UI only).
* E-mails are simulated and written to the log.
* NFR-007 (availability) and NFR-008 (automatic backup) are not implemented; they depend on the deployment.
* Tested with H2 on Windows. The MySQL profile and Linux were not tested.
* HTTP Basic login must be used over HTTPS in a real installation.
