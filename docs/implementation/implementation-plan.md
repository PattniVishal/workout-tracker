# Workout Tracker — Implementation Plan (MVP)

**Status:** Planning only. No application code, project generation, or Flyway SQL in this document.  
**Binding sources:** `mvp-decisions.md`, ADR-001–007, `database-design.md`, `class-diagram.md`, `sequence-diagrams.md`, `api-design.md`.  
**HLD:** Use for module map and SPA/API/PostgreSQL topology. **Do not implement** HLD §8 persisted `DISCARDED`, HLD §14 JWT alternative, or HLD §15 “cookie vs JWT / discard status vs delete” as open. Those are **superseded** (consistency review C-01). Follow **ADR-006** (HTTP-only server session) and **ADR-007** (physical discard).

**Out of scope for this build:** rest timer, streak, charts, PRs, Settings, password reset, email verification, duplicate routine, Progress screen, JWT, OAuth, account deletion, offline sync.

---

# 1. Implementation Strategy

Build **vertical slices** only after the platform and auth foundation exist. Each slice is backend-complete (migration → entity → service → API → tests) before the SPA consumes it.

**Order (minimize rework and cycles):**

```text
1. Monorepo layout + backend bootstrap + PostgreSQL + Flyway validate
2. Cross-cutting: errors, security filter, CSRF/CORS, CurrentUser
3. Users + register/login/logout/me   ← no JWT
4. Exercises (seed catalog + custom + archive)
5. Routines (depends on pickable exercises)
6. Active session (depends on routines + exercises)
     start / resume / log / complete / discard
7. History + previous performance (same WorkoutSession aggregate)
8. Dashboard read model (composes 5–7)
9. Frontend shell + auth, then screens in the same domain order
10. End-to-end happy path (PPL routine → log → complete → history)
```

**Why this order**

| Risk | Mitigation |
| --- | --- |
| Rework | Session graph and snapshots last among writes; schema of users/exercises/routines is stable first |
| Circular deps | `auth` ← nothing domain; `exercise` ← auth id only; `workout.routine` → `ExerciseService`; `workout.session` → routine + exercise; history → session repo only |
| Incomplete features | Do not start SPA logging until `POST /api/sessions` + set PATCH + complete exist |
| Premature FE/BE coupling | Contract is `docs/api/api-design.md`. Frontend starts after auth cookie + CORS work against a running API; mock as little as possible |

**Stack (fixed):** React/Vite SPA, Spring Boot modular monolith (`com.workouttracker`), PostgreSQL, Flyway, `ddl-auto=validate`, REST under `/api`, **server-side session cookie** (ADR-006).

**Cursor-sized units:** one phase ≈ one PR / one agent task. Do not implement “the whole workout module” in a single pass.

---

# 2. Repository and Project Structure

Keep a **single git repository** (docs already live at repo root).

```text
workout-tracker/
  docs/                 # existing product/architecture/database/api/review (do not regenerate)
  backend/              # Spring Boot (Java 21)
  frontend/             # Vite + React + TypeScript
  README.md             # added when projects are generated (not this task)
```

### `backend/`

| Path | Responsibility |
| --- | --- |
| `src/main/java/com/workouttracker/` | Application entry |
| `.../common/exception/` | `ApiExceptionHandler`, error JSON |
| `.../common/security/` | `SecurityConfig`, `CurrentUser`, `UserPrincipal` |
| `.../auth/{api,application,domain,infrastructure}/` | User + auth HTTP |
| `.../exercise/{api,application,domain,infrastructure}/` | Catalog + custom |
| `.../workout/routine/...` | Templates |
| `.../workout/session/...` | Active session aggregate |
| `.../workout/history/...` | History + previous performance + dashboard GET |
| `src/main/resources/application.yml` | Datasource, JPA validate, session, CORS |
| `src/main/resources/db/migration/` | Flyway |
| `src/test/java/...` | Unit + `@SpringBootTest` / slice tests |

**Rules:** Controllers → services → repositories. Workout must not use `ExerciseRepository` directly (use `ExerciseService.requirePickable`). No JWT classes.

### `frontend/`

| Path | Responsibility |
| --- | --- |
| `src/features/auth` | Login/register, cookie `credentials: 'include'` |
| `src/features/exercises` | Picker + desktop library |
| `src/features/routines` | List/editor |
| `src/features/session` | Active workout (mobile-first) |
| `src/features/history` | List/detail |
| `src/api/` | Fetch client, typed DTOs matching API design |
| `src/routes/` | React Router shells (no Settings, no Progress page) |

Do **not** generate these trees in this planning task.

---

# 3. Backend Implementation Phases

Phases are sequential unless noted. Endpoints match `docs/api/api-design.md`.

### Phase B0 — Backend bootstrap

- **Objective:** Runnable Spring Boot 3.x / Java 21 app with empty health, no domain yet.
- **Prerequisites:** JDK 21, Maven or Gradle (pick one and stick to it).
- **Packages:** `com.workouttracker` only.
- **Create:** `WorkoutTrackerApplication`, parent POM/build, `application.yml` stub, actuator or `/actuator/health` optional (not a product API).
- **DB:** none.
- **API:** none of `/api/*`.
- **Tests:** context loads.
- **Done:** `./mvnw spring-boot:run` (or Gradle) starts.

### Phase B1 — PostgreSQL + Flyway + JPA validate

- **Objective:** App fails if schema ≠ entities; migrations are the only DDL.
- **Prerequisites:** B0; local PostgreSQL.
- **Packages:** config only.
- **Create:** datasource config; `spring.jpa.hibernate.ddl-auto=validate`; Flyway enabled.
- **DB:** empty Flyway history until B3.
- **API:** none.
- **Tests:** testcontainers or Testcontainers-PostgreSQL later; for B1, Flyway migrate against local DB is enough.
- **Done:** `ddl-auto=validate` documented; no `update`/`create`.

### Phase B2 — Exception JSON + CORS foundation

- **Objective:** Consistent `{ status, code, message, fieldErrors }` (`api-design.md` §8–9).
- **Prerequisites:** B0.
- **Packages:** `common.exception`.
- **Create:** `ApiExceptionHandler`; domain exceptions (`NotFoundException`, `ConflictException`, `ForbiddenException`, `Validation` via MethodArgumentNotValidException).
- **DB:** none.
- **API:** error shape only.
- **Tests:** `@WebMvcTest` or `@SpringBootTest` throwing a fake controller if needed; otherwise wait until B3 has a real controller.
- **Done:** 400/401/403/404/409 mapping table implemented as exception types.

### Phase B3 — Security foundation (no JWT)

- **Objective:** Session cookie auth skeleton; CSRF + CORS for SPA origin; `CurrentUser`.
- **Prerequisites:** B2.
- **Packages:** `common.security`.
- **Create:** `SecurityConfig` (HttpOnly cookie, `Secure` in prod profiles); permit `/api/auth/register`, `/api/auth/login`; authenticate all other `/api/**`; CSRF for cookie API (consistency C-03); CORS `allowCredentials` + explicit origin. `UserPrincipal`, `CurrentUser`. **In-memory HTTP sessions for local/single instance (C-04).** No `JwtEncoder`.
- **DB:** none (servlet session not in application schema).
- **API:** unauthenticated `/api/**` → 401 (except public auth paths).
- **Tests:** security filter: anonymous GET `/api/auth/me` → 401.
- **Done:** ADR-006 behavior without login yet (login in B4).

### Phase B4 — Users and authentication

- **Objective:** Register (no session), login (sets cookie), logout, me.
- **Prerequisites:** B1–B3; migration V1 (see §4).
- **Packages:** `auth.*`.
- **Create:** `User` entity, `UserRepository`, `AuthService`, `AuthController`, request/response records (no `passwordHash` on responses). `PasswordEncoder` (BCrypt). Email normalize + unique.
- **DB:** V1 `app_user`.
- **API:** `POST /api/auth/register` (201, no cookie), `POST /api/auth/login` (200 + Set-Cookie), `POST /api/auth/logout` (204), `GET /api/auth/me` (200).
- **Tests:** unit hash never in DTO; integration register duplicate 409; login bad password 401; me with cookie 200; password not in JSON or logs (see §5).
- **Done:** Sequence §2–3 satisfied.

### Phase B5 — System exercises (seed)

- **Objective:** Global catalog readable by authenticated users; immutable via API.
- **Prerequisites:** B4; V2 migration + seed.
- **Packages:** `exercise.*` (entity/repo/service/list endpoint first).
- **Create:** `Exercise` entity, `ExerciseRepository`, `ExerciseService.listPickable`, `GET /api/exercises`. Seed a **small** curated list (content not specified by product — implementation may use common compounds: e.g. Bench Press, Squat, Deadlift, Overhead Press, Row, plus enough for PPL). Do not invent a content CMS.
- **DB:** V2 `exercise` + seed inserts (`created_by_user_id` null).
- **API:** `GET /api/exercises` (`q`, `muscleGroup` on **primary**).
- **Tests:** anonymous 401; authenticated list includes SYSTEM; PUT later phase returns 403 for system id.
- **Done:** Picker can show system rows.

### Phase B6 — Custom exercises + archive

- **Objective:** Owner create/edit/archive; picker = system + own non-archived custom.
- **Prerequisites:** B5.
- **Packages:** `exercise.*`.
- **Create:** `createCustom`, `updateCustom`, `archiveCustom`; `POST/PUT /api/exercises`, `POST /api/exercises/{id}/archive`.
- **DB:** no new tables (`archived_at` already on `exercise`).
- **API:** as api-design §3.
- **Tests:** other user’s custom 404; system PUT/archive 403; archived omitted from GET list; history later still references id.
- **Done:** `requirePickable` usable by workout.

### Phase B7 — Routines

- **Objective:** Full-payload create/update; unique exercise per routine; delete SET NULL origin later.
- **Prerequisites:** B6; V3.
- **Packages:** `workout.routine.*`.
- **Create:** `WorkoutRoutine`, `RoutineExercise`, `WorkoutRoutineService`, `WorkoutRoutineController`, DTOs. Update replaces entire `exercises` array.
- **DB:** V3 `workout_routine`, `routine_exercise` + uniques.
- **API:** `GET/POST /api/routines`, `GET/PUT/DELETE /api/routines/{id}`.
- **Tests:** duplicate `exerciseId` → 409; other user 404; delete routine does not delete users.
- **Done:** Can persist a PPL template.

### Phase B8 — Session start + resume (no logging yet)

- **Objective:** One `IN_PROGRESS`; start from routine (snapshots + planned empty sets) or empty; GET current 200/204.
- **Prerequisites:** B7; V4.
- **Packages:** `workout.session.*`.
- **Create:** `WorkoutSession`, `WorkoutExercise`, `WorkoutSet`, `WorkoutSessionStatus` enum (`IN_PROGRESS`/`COMPLETED` **only**), `WorkoutSessionService.startFromRoutine/startEmpty/getInProgress`, `WorkoutSessionController` POST/GET current. Copy `exercise.name` → `exerciseName` at start/add.
- **DB:** V4 session tables + **partial unique index** on `user_id WHERE status = 'IN_PROGRESS'`.
- **API:** `POST /api/sessions`, `GET /api/sessions/current`.
- **Tests:** second start 409; empty vs routine body; snapshot name ≠ later catalog rename (integration: rename exercise, GET current still old name); 204 when none.
- **Done:** Sequence §4–6.

### Phase B9 — Active logging (exercises and sets)

- **Objective:** Add/remove exercises and sets; PATCH weight/reps/completed; `assertInProgress`.
- **Prerequisites:** B8.
- **Packages:** `workout.session.*`.
- **Create:** remaining `WorkoutSessionService` mutators; nested endpoints under `/api/sessions/current`.
- **DB:** none.
- **API:** POST/DELETE exercises; POST/PATCH/DELETE sets.
- **Tests:** mutate with no current → 404; completed session cannot be reached via current; `completed: true` without values → 400; negative weight 400.
- **Done:** Sequence §7.

### Phase B10 — Complete and discard

- **Objective:** `IN_PROGRESS` → `COMPLETED` with `completedAt`; discard = **delete** aggregate (ADR-007).
- **Prerequisites:** B9.
- **Packages:** `workout.session.*`.
- **Create:** `complete()`, `discard()` → `repository.delete`. **No `DISCARDED` enum value.**
- **DB:** none (constraints already on status).
- **API:** `POST /api/sessions/current/complete` (409 `NO_COMPLETED_SETS`); `DELETE /api/sessions/current` (204).
- **Tests:** complete with zero completed sets 409; after complete GET current 204; discard removes rows (DB empty children); second user cannot discard first user’s session.
- **Done:** Sequence §8–9.

### Phase B11 — History

- **Objective:** List/detail/delete **COMPLETED** only.
- **Prerequisites:** B10.
- **Packages:** `workout.history.*` (`WorkoutHistoryService`, `WorkoutHistoryController`). Reuse `WorkoutSessionRepository`.
- **Create:** list DTO with `completedSetCount`; detail same session JSON; DELETE completed only.
- **DB:** none (indexes from V4).
- **API:** `GET/DELETE /api/history`, `GET /api/history/{id}`.
- **Tests:** IN_PROGRESS not in list; delete history 204; GET after delete 404; other user 404.
- **Done:** Product history + details.

### Phase B12 — Previous performance

- **Objective:** Latest completed session for current user + `exerciseId`; completed sets; snapshot name.
- **Prerequisites:** B11.
- **Packages:** `workout.history` (mapping on history controller for `GET /api/exercises/{exerciseId}/previous-performance`).
- **Create:** repository query; 204 if none.
- **DB:** use index `(exercise_id, workout_session_id)`.
- **API:** as api-design §6.
- **Tests:** other user’s sessions excluded; archived custom still returns history; live rename does not change snapshot in response.
- **Done:** Sequence §10.

### Phase B13 — Dashboard

- **Objective:** `GET /api/dashboard` without streak/week (C-05: owner = `WorkoutHistoryController` or thin `DashboardController` in `workout.history` package — **not** a fourth module).
- **Prerequisites:** B7, B8, B11.
- **Packages:** `workout.history`.
- **Create:** compose `getInProgress`, routine summaries, most recent completed, totals.
- **DB:** none.
- **API:** `GET /api/dashboard`.
- **Tests:** empty user zeros/nulls; `hasActiveSession` matches GET current.
- **Done:** Home API for SPA.

---

# 4. Database Migration Plan

Follow `.cursor/rules/04-database.mdc` naming. Logical grouping from `database-design.md`. **No SQL in this document.**

### V1 — `V1__create_app_user.sql`

| | |
| --- | --- |
| Tables | `app_user` |
| PK | `id` UUID |
| Unique | `email` |
| FK | none |
| Indexes | unique email |
| Cascade | n/a |
| Notes | `password_hash` NOT NULL; no token tables |

### V2 — `V2__create_exercise.sql`

| | |
| --- | --- |
| Tables | `exercise` |
| PK | `id` |
| FK | `created_by_user_id` → `app_user` **ON DELETE RESTRICT** |
| Check | `archived_at IS NULL OR created_by_user_id IS NOT NULL` |
| Indexes | `created_by_user_id`; `primary_muscle_group`; optional partial for non-archived custom |
| Seed | System rows (`created_by_user_id` NULL) — same migration or `V2_1__seed_system_exercises.sql` if seed is large |
| Cascade | none from user |

### V3 — `V3__create_workout_routine.sql`

| | |
| --- | --- |
| Tables | `workout_routine`, `routine_exercise` |
| FK | routine `user_id` → `app_user` **RESTRICT**; `routine_exercise.workout_routine_id` → routine **ON DELETE CASCADE**; `exercise_id` → `exercise` **RESTRICT** |
| Unique | `(workout_routine_id, exercise_id)`; `(workout_routine_id, position)` |
| Check | `position >= 1`; `planned_set_count >= 1` |
| Indexes | `workout_routine(user_id)` |

### V4 — `V4__create_workout_session.sql`

| | |
| --- | --- |
| Tables | `workout_session`, `workout_exercise`, `workout_set` |
| FK | session `user_id` → `app_user` **RESTRICT**; `origin_routine_id` → `workout_routine` **ON DELETE SET NULL**; `workout_exercise.workout_session_id` → session **ON DELETE CASCADE**; `exercise_id` → `exercise` **RESTRICT**; `workout_set.workout_exercise_id` → `workout_exercise` **ON DELETE CASCADE** |
| Unique | `(workout_session_id, position)`; `(workout_exercise_id, set_number)` |
| **One active workout** | **`CREATE UNIQUE INDEX ... ON workout_session (user_id) WHERE status = 'IN_PROGRESS'`** |
| Check | `status IN ('IN_PROGRESS','COMPLETED')`; completed_at null iff IN_PROGRESS; set completed implies weight/reps present; non-negative weight/reps |
| Indexes | completed history `(user_id, completed_at DESC) WHERE status = 'COMPLETED'`; `workout_exercise(exercise_id, workout_session_id)` |
| No | `DISCARDED` status; no `duration` column |

Do not add Spring Session JDBC tables unless/until scaling requires them (C-04).

---

# 5. Authentication and Security Implementation Plan

Implement **ADR-006 Option 1** only.

| Step | What |
| --- | --- |
| Registration | `AuthService.register`: validate DTO, lowercase email, `existsByEmail` → 409, BCrypt hash, save `User`, return DTO **without** hash, **do not** `HttpSession` |
| Password hashing | `PasswordEncoder` bean; encode on register; `matches` on login only |
| Login | Load by email; 401 on miss or mismatch (same message); `SecurityContext` + HTTP session; **Set-Cookie** HttpOnly; Secure on `prod` profile |
| Logout | `session.invalidate()`; 204 |
| Current user | `GET /api/auth/me` via `CurrentUser` |
| Ownership | Every workout/exercise/routine query includes `userId` from `CurrentUser`, never from body |
| Cookie | `HttpOnly`; `Secure` in production; `SameSite` per SPA/API topology (C-03) |
| CSRF | Enable for cookie-authenticated API; SPA sends CSRF header on POST/PUT/PATCH/DELETE (C-03) |
| CORS | Allow configured frontend origin + `allowCredentials: true` |
| AuthN vs AuthZ | Missing cookie → **401**; other user’s id → **404**; system exercise mutate → **403** |

**Password checklist**

- [ ] Never persist plaintext (`password_hash` only)
- [ ] Never map `passwordHash` to any response record
- [ ] Never log request bodies containing `password`
- [ ] Never log `password_hash`
- [ ] Login errors do not reveal whether email exists
- [ ] No JWT, refresh, or token JSON fields

---

# 6. Domain Module Implementation Order

```text
common (exception, security)
   ↓
auth (User + session cookie)
   ↓
exercise (catalog + custom)
   ↓
workout.routine
   ↓
workout.session (active + complete + discard)
   ↓
workout.history (history + previous performance)
   ↓
dashboard GET (same history package)
```

| Module | Depends on | Must not depend on |
| --- | --- | --- |
| **Identity/auth** | Spring Security, `app_user` | exercise, workout |
| **Exercise** | `CurrentUser` id only | workout |
| **Routine** | `ExerciseService.requirePickable` | session/history |
| **Workout session** | Routine **read** at start; Exercise **read** for snapshot/pickable | history package (history may call session **repository**) |
| **Workout history** | `WorkoutSessionRepository` | `WorkoutRoutineService` for display names (use snapshots) |
| **Dashboard** | History + routine list + `getInProgress` | new tables |

**Frontend order (after B4 cookie works):** auth pages → app shell (`/me`) → routines + exercise picker → active session → history → dashboard widgets. Do not build the gym logger UI before B9–B10.

---

# 7. Frontend Implementation Phases (after API slices)

Not in the user’s numbered list but required to avoid coupling: consume **only** enabled endpoints.

| Phase | Screens | Blocked until |
| --- | --- | --- |
| F0 | Vite + React + Router + TanStack Query + API client (`credentials: 'include'`) | B3 CORS |
| F1 | Login / register (no Settings) | B4 |
| F2 | Shell + logout + `GET /api/auth/me` | B4 |
| F3 | Exercise picker + custom create | B6 |
| F4 | Routine list/editor | B7 |
| F5 | Active workout (mobile-first) | B9–B10 |
| F6 | History list/detail | B11 |
| F7 | Dashboard home | B13 |
| F8 | Previous performance on logger | B12 |

---

# 8. Testing and Definition of “MVP coded”

Per `.cursor/rules/02-backend.mdc`: unit tests for session complete/discard/ownership; integration tests for auth cookie, second-start 409, unique routine exercise, snapshot stability.

**Release slice done when:** a new user can register, log in, create PPL from catalog + custom, start from routine, log sets, complete, see history, see previous performance on a second session — online, kg only, no rest timer/streak/charts.

---

# 9. Risks and Guardrails

| Risk | Guardrail |
| --- | --- |
| Implementing HLD `DISCARDED` | Enum has two values; discard is `delete` |
| JWT “for SPA” | Forbidden; cookie + CSRF |
| Hibernate `ddl-auto=update` | validate only |
| Frontend trusting ids | Always `CurrentUser` |
| Seed catalog bikeshedding | Small PPL-capable list; iterate in a later seed migration |
| Week/streak on dashboard | Do not add |

---

# 10. Next Actions After This Plan

1. Generate `backend/` Spring Boot project (B0) — **code starts here**, not in this file.  
2. Apply V1–V4 as each backend phase needs them.  
3. Generate `frontend/` when B3 CORS + B4 login exist.

No further product or architecture decisions are required to start B0.
