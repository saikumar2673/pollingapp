# Online Polling Platform

A Spring Boot polling platform where authenticated users can create polls, publish them, share them by URL, invite users to private polls, vote once per poll, update or withdraw votes while open, and view aggregate results according to each poll's visibility rules.

## Setup And Run

Prerequisites:

- Java 21
- Maven 3.9+
- SQL Server running locally on port `1433`
- A SQL Server login with permission to create/use the app database

Clone/open the project, then create the database:

```sql
CREATE DATABASE polling_platform;
```

From the project root, run:

```powershell
$env:DB_URL="jdbc:sqlserver://localhost:1433;databaseName=polling_platform;encrypt=true;trustServerCertificate=true"
$env:DB_USERNAME="sa"
$env:DB_PASSWORD="YourStrong!Passw0rd"
mvn spring-boot:run
```

Open the app:

```text
http://localhost:8080
```

The app uses Hibernate `ddl-auto: update` for local development, so tables are created automatically on startup.

Optional local SQL Server container:

```powershell
docker compose up -d
```

After the container starts, create the `polling_platform` database, then run the app. If login fails, update `DB_USERNAME` and `DB_PASSWORD` to match your SQL Server credentials.

## Tech Stack And Rationale

- Java 21: current LTS Java version with good Spring Boot support.
- Spring Boot 3: fast project setup, embedded Tomcat, production-style configuration.
- Spring MVC + Thymeleaf: simple server-rendered pages are enough for this product and avoid frontend build complexity.
- HTML, CSS, vanilla JavaScript: keeps the UI lightweight and easy to run locally.
- Spring Security: standard authentication/session handling and route protection.
- BCrypt password hashing: passwords are never stored in plaintext or reversible form.
- Spring Data JPA/Hibernate: concise persistence layer with transactions and entity relationships.
- SQL Server: durable relational storage with unique constraints, indexes, pagination, sorting, and realistic persistence across restarts.

SQL Server was chosen because the project benefits from relational integrity:

- `users.email` must be unique.
- one vote-set per user per poll is enforced with a unique vote constraint.
- duplicate invitations are prevented with a unique poll/invitee constraint.
- poll listing needs filtering, sorting, and pagination.

## Architecture Overview

The code is organized by feature, with controllers handling HTTP, services enforcing business rules, repositories handling persistence, and Thymeleaf templates rendering pages.

Key areas:

- `src/main/java/com/example/polling/PollingApplication.java`: application entry point.
- `config/SecurityConfig.java`: Spring Security route protection, login, logout, BCrypt encoder.
- `auth/`: registration, login support, current-user lookup.
- `user/`: user entity and repository.
- `poll/`: poll entities, lifecycle rules, access checks, listing, page controller.
- `invitation/`: private poll invite and revoke logic.
- `vote/`: vote-set replacement, withdrawal, and vote validation.
- `results/`: aggregate counts, percentages, and result visibility checks.
- `common/`: shared exceptions, global error handling, global model attributes.
- `src/main/resources/templates/`: Thymeleaf pages.
- `src/main/resources/static/`: CSS and JavaScript.
- `src/main/resources/application.yml`: SQL Server and JPA configuration.
- `docker-compose.yml`: optional SQL Server container.

The service layer is the main authority for business rules. UI controls are hidden where appropriate, but direct POST/API-style attempts are also rejected in service methods.

## Important Behavior

- All polling screens require authentication.
- Passwords are stored with BCrypt hashes.
- Login uses a server-side session cookie with a 30-minute inactivity timeout.
- New polls start as Draft.
- Draft polls are visible only to their creator.
- Public Draft polls do not appear in the public feed.
- Published polls become Open and lock options, type, and visibility.
- Open polls can accept votes from users with access.
- Closed polls are read-only and cannot be reopened.
- Polls with an expired `endAt` auto-close on the next read or write.
- Each user has one active vote-set per poll. Resubmitting replaces the previous selection.
- Users can withdraw their vote while a poll is Open.
- Public polls appear in the public feed after publishing.
- Private polls never appear in the public feed.
- Private polls are accessible only to the creator and invited users, even if someone guesses the share URL.
- Revoking an invitation removes future access immediately while the poll is Draft or Open.
- Once a poll is Closed, invitations cannot be revoked, so existing invitees keep access to final results.
- Individual vote choices are private. Other users, including the creator, only see aggregate results.
- Multi-choice percentages are calculated against total respondents, so they may total more than 100%.

## Main Pages

- `/register`: create account.
- `/login`: sign in.
- `/polls`: public feed.
- `/polls/my`: polls created by the signed-in user.
- `/polls/shared`: private polls shared with the signed-in user.
- `/polls/new`: create a draft poll.
- `/polls/{shareToken}`: poll detail and share URL.

## Quick Manual Test

Use three users:

```text
Alice: alice@example.com / password123
Bob: bob@example.com / password123
Charlie: charlie@example.com / password123
```

Recommended checks:

- Register, logout, and login again.
- Create a public poll as Alice, publish it, then confirm Bob sees it in `/polls`.
- Vote as Bob, change the vote, and confirm total respondents stays `1`.
- Withdraw Bob's vote and confirm counts decrement.
- Create a private poll as Alice and publish it.
- Confirm Charlie cannot access the private poll, even with the share URL.
- Invite Bob and confirm the poll appears in Bob's `/polls/shared`.
- Close the private poll and confirm Bob can still see final results.
- Confirm Alice can no longer revoke Bob after the private poll is closed.

## How AI Tools Were Used

This codebase was generated and iterated with assistance from OpenAI Codex inside the Codex desktop environment.

AI-generated or AI-assisted:

- Initial implementation plan.
- Spring Boot project structure.
- JPA entities, repositories, services, controllers.
- Thymeleaf templates, CSS, and small JavaScript helpers.
- README and local setup documentation.
- Debugging fixes for lazy-loading, shared-poll sorting, username display, and closed-poll invitation behavior.

Human-directed decisions:

- Use SQL Server instead of PostgreSQL.
- Prioritize implementation over unit/integration tests for the first pass.
- Require logged-in username display in the page header.
- Change invitation behavior so closed polls preserve invitee access and block revocation.

Reviewed, edited, or rejected:

- The implementation was repeatedly built with `mvn -DskipTests package`.
- Runtime errors reported from the console were reviewed and patched.
- A proposed implementation-plan update was rejected, so the README and code currently reflect the latest closed-poll invitation rule more accurately than the original plan document.

Worth flagging:

- The code is AI-assisted and should receive normal human code review before production use.
- Automated test coverage is intentionally not complete yet.

## Assumptions

- Users are registered directly in the app with name, email, and password.
- Invitation by email only supports existing registered users.
- A "team" is represented by all authenticated users in this single application instance.
- Email delivery is out of scope; invitations are visible in `Shared with me` after creation.
- Session-based authentication is acceptable.
- Local development can use Hibernate schema updates instead of managed migrations.
- The app is intended for a small-to-medium internal/team scenario, not internet-scale public polling.

## Trade-Offs

Given the 6-hour implementation budget, these were deliberately deprioritized:

- Full unit and integration test suite.
- Database migration tooling such as Flyway or Liquibase.
- REST API separation from server-rendered pages.
- Email notifications for invitations.
- Advanced UI polish and accessibility pass.
- Audit logs for admin/security review.
- Rate limiting and account lockout.
- Admin role or organization/team management.
- Optimized respondent-count caching for very large datasets.

The focus was placed on end-to-end functionality, lifecycle correctness, access enforcement, and vote-count integrity.

## Future Work

With more time, I would add:

- Unit tests for poll lifecycle, voting, results visibility, and invitation rules.
- MockMvc integration tests for authentication and direct URL tampering.
- Flyway migrations for deterministic SQL Server schema management.
- Better form-level validation messages instead of generic error pages for every failed action.
- Email notification or invite-by-email flow for unregistered users.
- Audit trail for poll publishing, closing, invite changes, and vote changes.
- Better UI components for filters, pagination, and result charts.
- CSRF/session hardening review and rate limiting.
- Optimized result aggregation for large polls.
- Deployment profile with production-safe `ddl-auto: validate`.
