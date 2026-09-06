# Architecture Decision Records (MVP)

This file records architecture decisions for the Workout Tracker MVP.

**Status values:** `ACCEPTED` — approved by project rules and/or the High-Level Design. `PROPOSED` — evaluated but not locked; an explicit choice is required before implementation.

**Sources:** `docs/architecture/high-level-design.md`, `docs/product/mvp-decisions.md`, `docs/product/mvp-spec-readiness.md`, `.cursor/rules/`. Product precedence: `mvp-decisions.md` overrides `feature.md`. This document does not invent product requirements or change the HLD.

**Constraints preserved in every relevant ADR:**

- Modular monolith; no independently deployable microservices
- No unnecessary infrastructure (no Kafka, gRPC, GraphQL, extra datastores for MVP)
- PostgreSQL is the system of record
- At most one persisted in-progress workout session per user
- Completed workout history remains historically accurate
- Routine changes or deletion must not corrupt completed sessions
- User-owned resources (routines, sessions, custom exercises, history) require authorization checks

---

## Index

| ID | Title | Status |
| --- | --- | --- |
| [ADR-001](#adr-001-modular-monolith-architecture) | Modular Monolith Architecture | ACCEPTED |
| [ADR-002](#adr-002-rest-api-between-react-frontend-and-spring-boot-backend) | REST API between React Frontend and Spring Boot Backend | ACCEPTED |
| [ADR-003](#adr-003-postgresql-as-the-primary-database) | PostgreSQL as the Primary Database | ACCEPTED |
| [ADR-004](#adr-004-flyway-for-database-schema-management) | Flyway for Database Schema Management | ACCEPTED |
| [ADR-005](#adr-005-separate-workout-routine-templates-from-workout-sessions) | Separate Workout Routine Templates from Workout Sessions | ACCEPTED |
| [ADR-006](#adr-006-authentication-strategy) | Authentication Strategy | ACCEPTED |
| [ADR-007](#adr-007-discarded-workout-session-persistence) | Discarded Workout Session Persistence | ACCEPTED |

---

## ADR-001: Modular Monolith Architecture

### Status

ACCEPTED

### Context

The application is a Workout Tracker with a bounded MVP: authentication, exercise catalog, routines, one persisted in-progress session per user, completed history, and previous-performance reads. Project rules require a modular monolith and forbid independently deployable microservices. The HLD organizes the backend into **auth**, **exercise**, and **workout** packages inside one Spring Boot process.

### Decision

Build a **single deployable Spring Boot application** whose code is partitioned by feature module (packages), not by network service. Modules expose application services to each other in-process. Controllers, services, repositories, domain types, and DTOs remain layered **within** a module. Other modules must not use another module’s repositories. Do not introduce microservices, service meshes, or inter-service RPC for MVP.

### Rationale

- Matches `.cursor/rules/01-architecture.mdc` and project context.
- One in-progress session, snapshots, and previous-performance queries span routines and completed sets; keeping them in one process avoids distributed transactions.
- A solo developer can run, test, and reason about one backend.
- Avoids unnecessary infrastructure (discovery, multiple databases, async buses).

### Alternatives Considered

| Alternative | Why not for MVP |
| --- | --- |
| Microservices (auth, exercise, workout as separate deployables) | Violates project rules; adds network failure modes and duplicate data ownership for sessions vs catalog. |
| Unstructured “one package” Spring app | Possible at tiny scale; harder to keep ownership of sessions vs catalog vs credentials clear. |
| Modular monolith plus a separate “progress” service | P1 charts/PRs are deferred; previous performance is a workout **read**. A fourth runtime is unnecessary. |

### Consequences

- Deployment boundary is one API process plus PostgreSQL (and a separately hosted SPA).
- Workout will be the largest module; splitting packages later is a refactor, not a new product.
- Historical accuracy and “one `IN_PROGRESS` session per user” are enforced in-process (and in the database), not via saga orchestration.
- Authorization stays in each module’s services using the authenticated `userId` (ADR-006, HLD §10).

### Risks or Operational Considerations

- Discipline is required so modules do not leak JPA entities across HTTP (DTOs at the API boundary).
- Do not “prepare” for microservices with extra layers that do not serve MVP.

---

## ADR-002: REST API between React Frontend and Spring Boot Backend

### Status

ACCEPTED

### Context

The frontend is a React (Vite) SPA. The backend is Spring Web. Users must log sets from a gym-oriented UI; the server is the source of truth for in-progress sessions (online-only). Project stack specifies REST, not GraphQL or gRPC.

### Decision

Expose a **JSON REST API** from Spring MVC controllers. The SPA uses a single API client and TanStack Query for server state. Request and response **DTOs** are independent of JPA entities. Do not add GraphQL, gRPC, WebSockets, or a BFF process for MVP.

### Rationale

- Explicit in project context and HLD system context (SPA → HTTPS REST → Spring Boot).
- Session logging is request/response CRUD plus a small lifecycle (start, complete, discard); REST resource + action endpoints are sufficient.
- TanStack Query fits HTTP cache/invalidation of routines, session, and history.
- No extra infrastructure (gateway, GraphQL server, message broker).

### Alternatives Considered

| Alternative | Why not for MVP |
| --- | --- |
| GraphQL | Not in the approved stack; adds a query layer without a product need for client-driven graphs. |
| gRPC / protobuf from the browser | Unnecessary protocol complexity; poor fit for a TypeScript SPA without extra tooling. |
| Server-rendered monolith (Thymeleaf only) | Contradicts the chosen React frontend and mobile-first SPA logging UX. |
| WebSocket for live session | Online persistence is HTTP mutations; no collaborative editing requirement. |

### Consequences

- CORS with credentials and CSRF protection must be configured for the SPA origin (ADR-006).
- Versioning can stay implicit (`/api/...`) until a breaking change exists.
- Previous performance is a REST read on workout data, not a streaming API.
- Authorization is enforced per request on user-owned resources.

### Risks or Operational Considerations

- Chatty logging (each set save) is acceptable at MVP scale; batching is not required by product.
- Controllers must remain thin; business rules (complete gate, one in-progress session) stay in services.

---

## ADR-003: PostgreSQL as the Primary Database

### Status

ACCEPTED

### Context

The domain is relational: users, global vs custom exercises, routines with ordered exercises and planned set counts, sessions with lifecycle, snapshotted names, and sets with completion flags. Project rules mandate PostgreSQL. Product requires historically accurate completed sessions and at most one in-progress session per user.

### Decision

Use **PostgreSQL as the only system of record** for application data. All durable user, catalog, routine, and session state lives there. Do not add a second database, cache cluster, or search engine for MVP.

### Rationale

- Approved stack and HLD §3 / §13.
- Foreign keys, uniqueness (email as login identifier; at most one `IN_PROGRESS` session per user), and constraints can enforce invariants the database is allowed to own.
- Snapshots live in session tables, not as a live join to the current routine—so routine edit/delete cannot rewrite history if modeled that way (ADR-005).
- Online resume of an in-progress session is a PostgreSQL read, not a client-only store.

### Alternatives Considered

| Alternative | Why not for MVP |
| --- | --- |
| MongoDB / document store for sessions | Project forbids replacing PostgreSQL; relational sets and ownership checks are a poor fit for an extra store. |
| SQLite | Insufficient for a separately deployed API and multi-device resume. |
| PostgreSQL plus Redis for sessions | Unnecessary infrastructure; Spring session and workout session data can live in PostgreSQL. |
| Dual-write to a warehouse for “progress” | Previous performance is a query over completed sets; PRs/charts are deferred. |

### Consequences

- Flyway (ADR-004) evolves this schema; Hibernate `ddl-auto` must not create or update schema in controlled environments.
- Authorization still happens in services: a user must only read/write rows they own (global catalog excepted for read).
- Discarded in-progress sessions are physically deleted and never appear as history (ADR-007).

### Risks or Operational Considerations

- Backup and access control apply to one database; the API is the only client in production.
- Do not store password hashes in API payloads; they may exist only as credential columns for auth.

---

## ADR-004: Flyway for Database Schema Management

### Status

ACCEPTED

### Context

Schema will include users, exercises, routines, sessions, snapshots, and constraints (including one in-progress session per user). Hibernate auto-update would hide history-risking changes. Project database rules require Flyway and `ddl-auto=validate` in controlled environments.

### Decision

All schema changes go through **versioned Flyway migrations**. Naming should describe the change (as in the project examples). Application startup in controlled environments **validates** the schema against the entity mapping; it does not create, create-drop, or update the schema via Hibernate.

### Rationale

- Required by `.cursor/rules/04-database.mdc`.
- Migrations are reviewable and repeatable for history-sensitive tables (session snapshots).
- Catalog seed data can ship as a migration or a clearly named follow-on migration without a second data platform.

### Alternatives Considered

| Alternative | Why not |
| --- | --- |
| Hibernate `ddl-auto=update` | Forbidden in controlled environments; can drift and mask destructive changes to history tables. |
| Liquibase | Not the project standard; no benefit that justifies a second tool. |
| Manual SQL on the server | Not repeatable across environments. |

### Consequences

- Destructive changes must call out data risk (project rule).
- Entity mappings and migrations must stay aligned or the app fails fast on validate.
- One in-progress session and historical snapshot columns are introduced in migrations, not by ad hoc DBA edits.

### Risks or Operational Considerations

- Migration order is linear; avoid rewriting applied migrations.
- Seed catalog content is data, not architecture; still applied via the same pipeline.

---

## ADR-005: Separate Workout Routine Templates from Workout Sessions

### Status

ACCEPTED

### Context

Product glossary: a **routine** is a reusable template (name, description, ordered exercises, planned set count). A **session** is a logged workout with persisted lifecycle status `IN_PROGRESS` or `COMPLETED` (see ADR-007 for discard). Users may edit or delete routines after logging. Completed history must stay accurate. At most one `IN_PROGRESS` session per user. Starting from a routine copies name, exercises, and planned set counts; empty start has no template.

`feature.md` §20 linked session exercises only by `exerciseId`. `mvp-decisions.md` requires at least a snapshotted exercise **name** and grouping by stable identity when available. The HLD copies name (and identity) when an exercise is **placed on a session**.

### Decision

Persist **routines** and **sessions** as separate aggregates in the workout module:

- **Routine** — current template; user-owned; unique exercise per routine; planned set count only (no target weight/reps).
- **Session** — logged instance; user-owned; optional origin routine id; lifecycle status; `startedAt` / `completedAt`; session exercises with **snapshotted name** plus exercise identity when resolved; sets with completion flag.

Starting a session **copies** template data into the session graph. Later routine edits do not mutate existing sessions. **Deleting a routine must not delete or rewrite completed sessions** (origin may be cleared). History and previous-performance queries use **completed session** data only, not the live routine. Incomplete sets are not treated as completed workout data.

Do not use the live catalog or routine row as the display source for completed history.

### Rationale

- Implements historical preservation and routine-delete rules without a data warehouse.
- Enables one persisted in-progress session (session row in PostgreSQL) independent of whether the user still has that routine.
- Previous performance is “latest completed session containing this exercise identity,” which is a session query.
- Keeps modules simple: both aggregates in **workout**, not a separate history microservice.

### Alternatives Considered

| Alternative | Why not |
| --- | --- |
| Session as a pointer to the current routine only | Routine edits and deletes would corrupt or erase history. Violates product and database rules. |
| Event sourcing of every set | Unnecessary complexity for MVP; PostgreSQL rows are enough. |
| Separate history database | Extra infrastructure; same PostgreSQL can hold frozen session graphs. |
| Unique exercise per session (same as routine) | Product uniqueness is **routine-only**; do not invent a session constraint. |

### Consequences

- Slight denormalization (names on session exercises).
- Authorization: users may only access their routines and their sessions.
- Complete gate and uniqueness of `IN_PROGRESS` apply to the session aggregate, not the routine.
- Custom exercise archive hides pickers; snapshots and ids on old sessions remain.

### Risks or Operational Considerations

- Implementers must not `ON DELETE CASCADE` from routine to session.
- Snapshot at **add-to-session** time (including empty-session adds), not only at complete, so mid-session catalog edits cannot rewrite the logged name.

---

## ADR-006: Authentication Strategy

### Status

ACCEPTED

### Context

The approved product requires **email and password** signup/login/logout, private per-user data, and resume of an in-progress **workout** after browser refresh, close, or another device. Password reset and email verification are deferred.

- SPA and API are **separately deployed** (HLD §13), so the browser origin may differ from the API origin.
- Workout session state lives in PostgreSQL (ADR-003, ADR-005); auth only identifies the user. Multi-device resume of the **workout** does not require storing the workout in the browser.
- Remaining logged in after a **page refresh** requires the **auth** credential to survive reload so the user can resume without re-entering credentials (workout data remains on the server regardless).
- Spring Security is the approved framework. No OAuth providers in MVP.
- User-owned resources require authorization on every request using the authenticated `userId`.

### Decision

Use **Option 1 — HTTP-only server-side session**.

- Spring Security manages authentication with an **HTTP-only**, `Secure` session cookie.
- The server stores session state keyed by session id. For MVP, session storage may use in-memory sessions or **Spring Session JDBC** on the existing PostgreSQL instance (not a separate datastore).
- The SPA sends credentials on login; subsequent API requests include the session cookie automatically when configured with `credentials: 'include'`.
- Logout **invalidates the server session** and clears the client auth state.
- Do **not** use JWT bearer tokens or a JWT access/refresh cookie pair for MVP.

### Rationale

- Aligns with the HLD preference (`high-level-design.md` §14) and explicit architecture approval.
- HTTP-only cookies keep the session credential out of JavaScript, reducing XSS token theft compared to `localStorage` bearer tokens.
- Page refresh and login from another device keep the user authenticated without custom client token storage.
- Server-side logout is straightforward (session invalidate).
- Fits the modular monolith: auth stays in the **auth** module; no separate auth service or token infrastructure.
- Workout logging, history, and one in-progress session per user remain in PostgreSQL as the system of record; the auth session only identifies the user.

### Alternatives Considered

| Alternative | Why not for MVP |
| --- | --- |
| **Option 2 — JWT bearer token** | Poor fit for “stay logged in across refresh” without `localStorage` (XSS risk) or extra client logic. Logout requires token denylist or client-only delete. |
| **Option 3 — JWT access + refresh in HTTP-only cookies** | More moving parts (rotation, reuse detection, CSRF on refresh) than MVP needs. |
| OAuth / Google / Apple sign-in | Deferred product feature. |
| Magic links / email OTP | Requires email infrastructure; verification is deferred. |
| Separate auth provider (Keycloak, Cognito) | Extra infrastructure; violates simplicity constraint. |

### Consequences

- **Frontend:** API client uses `credentials: 'include'` on all requests to the API origin. TanStack Query and the auth layer rely on the session cookie, not `Authorization` headers or `localStorage`.
- **Backend:** Spring Security filter chain; session creation on login; session invalidation on logout; CSRF protection on state-changing REST calls when using cookie-based auth with a cross-origin SPA.
- **CORS:** API must allow the SPA origin and `Access-Control-Allow-Credentials: true`.
- **Cookies:** `Secure` in production; `SameSite` and `Domain` must match the deployment topology (same-site vs cross-site SPA/API).
- Passwords hashed with a slow encoder (e.g. BCrypt); hashes never appear in APIs or logs.
- Email is unique as the login identifier.
- Every routine, session, custom exercise, and history access is authorized by owner `userId` in services.
- Horizontal scaling of multiple API instances requires shared session storage (e.g. Spring Session JDBC on PostgreSQL) or sticky sessions; not required for initial MVP deployment.

### Risks or Operational Considerations

- Cross-origin SPA + cookie auth requires careful **CSRF** configuration; do not skip this analysis at implementation.
- Cookie `SameSite=None; Secure` may be needed if SPA and API are on different sites; test login and mutating requests in the target deployment layout.
- Losing the auth cookie (expiry, logout, cleared cookies) does **not** delete the in-progress workout in PostgreSQL; the user may log in again and resume via `GET` in-progress session.
- Session fixation and timeout policies are implementation details of Spring Security configuration, not new product requirements.

---

## ADR-007: Discarded Workout Session Persistence

### Status

ACCEPTED

### Context

Product allows a user to **discard** an in-progress workout session. Discarded work must not appear in history and is not a completed record. The HLD described a `DISCARDED` lifecycle state as one option; an open architecture question remained whether discard is a persisted status or a physical delete.

This ADR resolves that question for database and API design.

### Decision

When a user discards an in-progress workout session:

- The session row is **physically deleted** from PostgreSQL.
- All child **workout exercise** and **workout set** rows for that session are **physically deleted** (cascade delete on the session aggregate).
- **`DISCARDED` is not stored** as a persistent workout session status.
- The persisted session lifecycle for MVP is limited to **`IN_PROGRESS`** and **`COMPLETED`** only.
- Discarded workouts do not appear in history and **cannot be restored**.
- This MVP does **not** require audit history, abandoned-workout analytics, or undo/recovery for discarded sessions.

Discard is a destructive operation on the in-progress aggregate, not a transition to a third stored state.

### Rationale

- Matches product intent: discard ends the live session without creating history.
- Simplifies schema and queries: history lists only `COMPLETED` sessions; no filter to exclude `DISCARDED` rows.
- Frees the “one in-progress session per user” slot immediately after delete.
- Avoids retaining rows that have no product use (no analytics, audit, or undo in MVP).

### Alternatives Considered

| Alternative | Why not for MVP |
| --- | --- |
| Persist `DISCARDED` status on the session row | Extra status, indexes, and query filters with no product requirement for audit, analytics, or restore. |
| Soft-delete (archived flag) | Same as above; implies recoverability the product does not offer. |
| Retain session graph with `DISCARDED` for future analytics | Explicitly out of scope for this MVP. |

### Consequences

- Database design uses a **status** column (or equivalent) with values **`IN_PROGRESS`** and **`COMPLETED`** only.
- Discard API performs **delete** of the session aggregate (session, session exercises, session sets) inside a transaction, authorized by session owner.
- History and previous-performance queries target **`COMPLETED`** sessions only; discarded data is absent by construction.
- Uniqueness of at most one `IN_PROGRESS` session per user is enforced on rows that exist; after discard, no row remains until the user starts again.
- Flyway migrations must define `ON DELETE CASCADE` (or equivalent service-level delete) from session to children, **not** from routine to session (ADR-005).

### Risks or Operational Considerations

- Discard is irreversible; UX may warn the user, but no server-side undo is required by product.
- Physical delete is not a substitute for completed-session delete: completed history remains a separate user action on `COMPLETED` rows.
- If abandoned-workout analytics are needed later, a new ADR would introduce retention or status—out of scope for MVP.

---

# Open Architecture Decisions

Product non-blocking questions (dashboard “this week,” Progress page, muscle-filter matching, previous-performance UI emphasis) are not architecture decisions and are not listed here.

**No open architecture decisions currently block database design.**

All ADRs in this file (ADR-001 through ADR-007) are ACCEPTED. Schema and API contracts are still to be designed in later documents.
