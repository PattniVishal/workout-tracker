# Workout Tracker — Sequence Diagrams (MVP)

**Status:** Logical interaction flows for the approved modular monolith.  
**Not in this document:** Java source, Flyway SQL, REST path catalogs, or UI mockups.

**Precedence:** `mvp-decisions.md` overrides `feature.md`. Sequences follow `class-diagram.md`, `database-design.md`, and `architecture-decisions.md` (ADR-001 through ADR-007).

---

# 1. Scope and Conventions

These diagrams show **how approved components collaborate** on P0 flows. They are **logical** interactions, not generated call stacks or line-for-line method names.

**Layers shown (when relevant):**

| Layer | Typical participants |
| --- | --- |
| Client | Browser / Frontend (React SPA) |
| API | `AuthController`, `WorkoutSessionController`, `WorkoutHistoryController`, Bean Validation |
| Security | `CurrentUser`, Spring Security session boundary (`SecurityConfig`) |
| Application | `AuthService`, `WorkoutSessionService`, `WorkoutHistoryService`, `ExerciseService` |
| Domain | `User`, `WorkoutRoutine`, `WorkoutSession`, `WorkoutExercise`, `WorkoutSet` |
| Persistence | `UserRepository`, `WorkoutRoutineRepository`, `WorkoutSessionRepository`, `ExerciseRepository` |
| Database | PostgreSQL |

**Intentionally abstracted:** `DispatcherServlet`, filter chain internals, `EntityManager` flush, Hibernate dirty checking, CSRF token round-trips (assumed configured per ADR-006), connection pooling, and exact HTTP status codes except where architecture already names them (e.g. 409 on second active session, 404 for other-user resources).

**Frontend** is a single participant. TanStack Query invalidation is omitted.

**Authentication:** stateful server-side HTTP session, **HTTP-only** and **Secure** cookie. **No JWT, access token, or refresh token.**

**Workout persistence:** `IN_PROGRESS` and `COMPLETED` only. Discard is **physical delete**, not a `DISCARDED` status.

---

# 2. User Registration

**Does registration automatically log the user in?** Product lists **sign up** and **log in** as separate capabilities. Class design has distinct `register` and `login`. No approved document requires creating an HTTP session on signup. This sequence **creates the account only**. Session establishment is §3 (login).

```mermaid
sequenceDiagram
  actor User
  participant FE as Frontend
  participant API as AuthController
  participant Val as BeanValidation
  participant Svc as AuthService
  participant Repo as UserRepository
  participant DB as PostgreSQL

  User->>FE: Submit display name, email, password
  FE->>API: Register request DTO
  API->>Val: Validate DTO
  alt Invalid input
    Val-->>API: Constraint violations
    API-->>FE: Validation failure
  else Valid
    API->>Svc: register(dto)
    Svc->>Svc: Normalize email
    Svc->>Repo: existsByEmail
    Repo->>DB: SELECT app_user
    alt Email already used
      Repo-->>Svc: true
      Svc-->>API: Conflict
      API-->>FE: Duplicate email
    else Email available
      Repo-->>Svc: false
      Svc->>Svc: PasswordEncoder.hash(password)
      Svc->>Repo: save(User)
      Repo->>DB: INSERT app_user
      DB-->>Repo: persisted
      Svc-->>API: User created (no password hash)
      API-->>FE: Success (not authenticated)
      Note over FE,DB: No session cookie. User logs in separately.
    end
  end
```

DB unique on `email` is a backstop if two registers race.

---

# 3. Login and Server-Side Session Establishment

Physical storage of the servlet/Spring session (in-memory vs Spring Session JDBC) is **not decided** in the application schema and is **not shown** as a domain table.

```mermaid
sequenceDiagram
  actor User
  participant FE as Frontend
  participant API as AuthController
  participant Sec as SpringSecurityBoundary
  participant Svc as AuthService
  participant Repo as UserRepository
  participant DB as PostgreSQL

  User->>FE: Submit email, password
  FE->>API: Login (credentials include)
  API->>Svc: login(email, password)
  Svc->>Repo: findByEmail
  Repo->>DB: SELECT app_user
  alt Unknown user or bad password
    Repo-->>Svc: missing or hash mismatch
    Svc->>Svc: PasswordEncoder.matches
    Svc-->>API: Authentication failed
    API-->>FE: Unauthenticated
  else Credentials valid
    Svc->>Svc: PasswordEncoder.matches success
    Svc->>Sec: Establish HttpSession
    Note over Sec: Server-side session id stored by Spring Security
    Sec-->>FE: Set-Cookie HttpOnly Secure session cookie
    Svc-->>API: Current user profile DTO
    API-->>FE: Success plus cookie
  end
```

Later API calls send the cookie with `credentials: 'include'`. Logout invalidates the **server** session. Workout rows in PostgreSQL are unchanged.

---

# 4. Start Workout from Routine

Authenticated. Cookie omitted after this section unless security is the point.

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant API as WorkoutSessionController
  participant CU as CurrentUser
  participant Svc as WorkoutSessionService
  participant SessRepo as WorkoutSessionRepository
  participant RoutRepo as WorkoutRoutineRepository
  participant ExSvc as ExerciseService
  participant Domain as WorkoutSession
  participant DB as PostgreSQL

  FE->>API: Start from routineId
  API->>CU: id()
  CU-->>API: userId
  API->>Svc: startFromRoutine(userId, routineId)

  Svc->>SessRepo: findByUserIdAndStatus(IN_PROGRESS)
  SessRepo->>DB: SELECT workout_session
  alt Already IN_PROGRESS
    SessRepo-->>Svc: present
    Svc-->>API: Conflict
    Note over API: Architecture: second start is a conflict (409)
    API-->>FE: Cannot start second active workout
  else No active session
    SessRepo-->>Svc: empty
    Svc->>RoutRepo: findByIdAndUserId(routineId, userId)
    RoutRepo->>DB: SELECT workout_routine plus routine_exercise
    alt Routine missing or not owned
      RoutRepo-->>Svc: empty
      Svc-->>API: Not found
    else Owned routine loaded
      loop Each RoutineExercise
        Svc->>ExSvc: requirePickable(exerciseId, userId)
        ExSvc-->>Svc: Exercise current name and id
      end
      Svc->>Domain: startFromRoutine copy name origin snapshot sets
      Note over Domain: exerciseName snapshotted. originRoutineId stored. Independent of later routine edits.
      Svc->>SessRepo: save(session)
      SessRepo->>DB: INSERT session exercises sets
      Note over DB: Partial unique index on user_id WHERE IN_PROGRESS
      SessRepo-->>Svc: persisted graph
      Svc-->>API: Session response DTO
      API-->>FE: Active workout
    end
  end
```

After this point, editing or deleting the **routine** does not rewrite the session graph. Completing later stores this copy as history (`COMPLETED`), still independent of the template.

---

# 5. Start Empty Workout

No routine load, no planned-set copy, `originRoutineId` null. Name comes from the request. Exercises may be added later.

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant API as WorkoutSessionController
  participant CU as CurrentUser
  participant Svc as WorkoutSessionService
  participant SessRepo as WorkoutSessionRepository
  participant Domain as WorkoutSession
  participant DB as PostgreSQL

  FE->>API: Start empty workout with name
  API->>CU: id()
  API->>Svc: startEmpty(userId, name)
  Svc->>SessRepo: findByUserIdAndStatus(IN_PROGRESS)
  alt Already IN_PROGRESS
    Svc-->>API: Conflict
    API-->>FE: Cannot start second active workout
  else Slot free
    Svc->>Domain: startEmpty(userId, name)
    Note over Domain: IN_PROGRESS. No children yet. No origin routine.
    Svc->>SessRepo: save(session)
    SessRepo->>DB: INSERT workout_session
    Svc-->>API: Session DTO
    API-->>FE: Active empty workout
  end
```

---

# 6. Resume Active Workout

Product: in-progress sessions are persisted and resumable. If none exists, **start** is the path (HLD). No approved “create empty session on GET.” This sequence returns **absence** of an active session; it does not invent an HTTP body shape.

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant API as WorkoutSessionController
  participant CU as CurrentUser
  participant Svc as WorkoutSessionService
  participant SessRepo as WorkoutSessionRepository
  participant DB as PostgreSQL

  FE->>API: Get in-progress session
  API->>CU: id()
  API->>Svc: getInProgress(userId)
  Svc->>SessRepo: findByUserIdAndStatus(IN_PROGRESS)
  SessRepo->>DB: SELECT session plus ordered exercises and sets
  alt Found IN_PROGRESS
    SessRepo-->>Svc: WorkoutSession graph
    Svc-->>API: Active session DTO
    API-->>FE: Resume UI
  else None
    SessRepo-->>Svc: empty
    Svc-->>API: No active session
    API-->>FE: Absence (user may start routine or empty)
  end
```

Unauthenticated callers never reach the service (`CurrentUser` / security filter → 401).

---

# 7. Log or Update a Workout Set

One representative mutation (weight/reps and optional mark completed). Add/remove set or exercise follows the same **owner + IN_PROGRESS** gate.

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant API as WorkoutSessionController
  participant CU as CurrentUser
  participant Svc as WorkoutSessionService
  participant SessRepo as WorkoutSessionRepository
  participant Session as WorkoutSession
  participant Set as WorkoutSet
  participant DB as PostgreSQL

  FE->>API: Update set weight reps optional complete
  API->>CU: id()
  API->>Svc: updateSet or markSetCompleted(userId, ids, values)
  Svc->>SessRepo: findByUserIdAndStatus(IN_PROGRESS)
  alt No IN_PROGRESS or ids not in that graph
    Svc-->>API: Not found or not mutable
    Note over Svc: COMPLETED sessions are not loaded for mutation
  else Owned IN_PROGRESS
    Svc->>Session: assertInProgress()
    Svc->>Set: applyLog and or markCompleted()
    Svc->>SessRepo: save(session)
    SessRepo->>DB: UPDATE workout_set
    Svc-->>API: Session DTO
    API-->>FE: Updated active workout
  end
```

A `COMPLETED` session is only read via `WorkoutHistoryService` or deleted as a history item — **not** updated here.

---

# 8. Complete Workout

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant API as WorkoutSessionController
  participant CU as CurrentUser
  participant Svc as WorkoutSessionService
  participant SessRepo as WorkoutSessionRepository
  participant Session as WorkoutSession
  participant DB as PostgreSQL

  FE->>API: Complete workout
  API->>CU: id()
  API->>Svc: complete(userId)
  Svc->>SessRepo: findByUserIdAndStatus(IN_PROGRESS)
  alt None
    Svc-->>API: Not found
  else Loaded
    Svc->>Session: complete()
    alt No completed set
      Session-->>Svc: Reject
      Svc-->>API: Business rule failure
    else At least one completed set
      Note over Session: IN_PROGRESS to COMPLETED. Set completedAt. Duration derived.
      Svc->>SessRepo: save(session)
      SessRepo->>DB: UPDATE status COMPLETED completed_at
      Note over DB: Partial unique IN_PROGRESS slot now free
      Svc-->>API: Completed session DTO
      API-->>FE: Summary / history
    end
  end
```

After `COMPLETED`, routine and `Exercise` updates do not rewrite this graph. History lists this row; logging APIs refuse mutation.

---

# 9. Discard Active Workout

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant API as WorkoutSessionController
  participant CU as CurrentUser
  participant Svc as WorkoutSessionService
  participant SessRepo as WorkoutSessionRepository
  participant DB as PostgreSQL

  FE->>API: Discard active workout
  API->>CU: id()
  API->>Svc: discard(userId)
  Svc->>SessRepo: findByUserIdAndStatus(IN_PROGRESS)
  alt None or not owned
    Svc-->>API: Not found
  else Owned IN_PROGRESS
    Svc->>SessRepo: delete(session)
    SessRepo->>DB: DELETE workout_session
    Note over DB: CASCADE deletes workout_exercise and workout_set
    Note over Svc,DB: IN_PROGRESS to physical deletion. No DISCARDED row.
    Svc-->>API: Success empty
    API-->>FE: No active workout
  end
```

Not restorable. Not in history. Completing after discard is impossible.

---

# 10. Previous Performance Lookup

Called from the active-workout UI. Implementation lives on `WorkoutHistoryService` (class-diagram). Archived custom exercises still have `exercise.id`; snapshots keep historical **names**.

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant API as WorkoutHistoryController
  participant CU as CurrentUser
  participant Hist as WorkoutHistoryService
  participant SessRepo as WorkoutSessionRepository
  participant DB as PostgreSQL

  FE->>API: Previous performance for exerciseId
  API->>CU: id()
  API->>Hist: getPreviousPerformance(userId, exerciseId)
  Hist->>SessRepo: Latest COMPLETED for userId containing exerciseId
  SessRepo->>DB: Query workout_session join workout_exercise
  alt No matching completed session
    SessRepo-->>Hist: empty
    Hist-->>API: No previous performance
    API-->>FE: Absence
  else Found
    SessRepo-->>Hist: Session plus WorkoutExercise snapshot and completed sets
    Note over Hist: Filter is_completed true. Use exerciseName snapshot not live Exercise.name
    Hist-->>API: Previous performance DTO
    API-->>FE: Show prior kg times reps
  end
```

Only `userId` from `CurrentUser`. Another user’s completed sessions are never queried. Archive of the catalog row does not remove these `COMPLETED` rows.

---

# 11. Authorization and Cross-User Access Pattern

Representative: fetch or update another user’s **routine** (same pattern for custom exercise and session: `findByIdAndUserId` / `findByIdAndCreatedByUserId`).

Class design: empty owner-scoped lookup → **not found** (do not leak that the id exists). Exact HTTP mapping is not a full API contract here.

```mermaid
sequenceDiagram
  participant FE as Frontend
  participant API as WorkoutRoutineController
  participant CU as CurrentUser
  participant Svc as WorkoutRoutineService
  participant Repo as WorkoutRoutineRepository
  participant DB as PostgreSQL

  FE->>API: Get or update routineId belonging to someone else
  API->>CU: id()
  Note over CU: Owner is authenticated user not the id in the URL
  API->>Svc: get or update(currentUserId, routineId)
  Svc->>Repo: findByIdAndUserId(routineId, currentUserId)
  Repo->>DB: SELECT where id and user_id
  DB-->>Repo: no row
  Repo-->>Svc: empty
  Svc-->>API: Not found
  API-->>FE: Not found
  Note over FE,DB: Frontend ids are lookups not proof of ownership
```

Security filter already rejected missing cookies (401) before this. Custom exercise mutate uses owner id on `Exercise`. Session mutate uses `IN_PROGRESS` + `userId`.

---

# 12. Concurrency / One Active Workout Consideration

Two overlapping **start** calls for the same user. No retry loop is specified; the losing attempt fails.

```mermaid
sequenceDiagram
  participant FE1 as FrontendAttemptA
  participant FE2 as FrontendAttemptB
  participant Svc as WorkoutSessionService
  participant DB as PostgreSQL

  par Attempt A
    FE1->>Svc: startFromRoutine or startEmpty
    Svc->>DB: SELECT IN_PROGRESS none
    Svc->>DB: INSERT IN_PROGRESS
    DB-->>Svc: OK
  and Attempt B
    FE2->>Svc: startFromRoutine or startEmpty
    Svc->>DB: SELECT IN_PROGRESS may still be empty
    Svc->>DB: INSERT IN_PROGRESS
    alt Partial unique index violation
      DB-->>Svc: Unique violation
      Svc-->>FE2: Conflict conceptually
    else Application saw existing row first
      Svc-->>FE2: Conflict conceptually
    end
  end
```

**Application check** avoids most collisions and yields a clear conflict. **PostgreSQL** `UNIQUE (user_id) WHERE status = 'IN_PROGRESS'` is the integrity guarantee if both checks pass. The winner has the only `IN_PROGRESS` row; the loser does not create a second active workout. UX copy for resume vs discard is product-open and not shown.

---

# 13. Consistency with Other Architecture Documents

| Sequence flow | Class-diagram components | Database components | Key architecture decision |
| --- | --- | --- | --- |
| Registration | `AuthController`, `AuthService`, `User`, `UserRepository` | `app_user` | Email/password account; no JWT; no auto-session unless later specified |
| Login | `AuthService`, `UserRepository`, Spring Security boundary, `CurrentUser` | `app_user` (lookup only) | ADR-006 HTTP-only server-side session cookie |
| Start from routine | `WorkoutSessionService`, `WorkoutRoutineRepository`, `ExerciseService`, `WorkoutSession` aggregate | `workout_session`, `workout_exercise` (snapshot), `workout_set`, `workout_routine` read | ADR-005 copy at start; one `IN_PROGRESS` (partial unique) |
| Start empty | `WorkoutSessionService`, `WorkoutSession` | `workout_session` only at insert | Same one-active rule; no origin routine |
| Resume | `WorkoutSessionService`, `WorkoutSessionRepository` | `workout_session` + children `IN_PROGRESS` | Server is source of truth (ADR-003) |
| Log set | `WorkoutSessionService`, `WorkoutSession.assertInProgress`, `WorkoutSet` | `workout_set` | Completed sessions not mutated |
| Complete | `WorkoutSession.complete`, `WorkoutSessionRepository.save` | status `COMPLETED`, `completed_at` | Lifecycle `IN_PROGRESS` → `COMPLETED` |
| Discard | `WorkoutSessionService.discard`, `delete` | `DELETE` session CASCADE children | ADR-007 physical delete; no `DISCARDED` |
| Previous performance | `WorkoutHistoryService`, `WorkoutSessionRepository` | `COMPLETED` + `exercise_id` + `is_completed` | Decision 18; snapshot name |
| Cross-user | `findByIdAndUserId`, `CurrentUser` | owner columns | HLD / class-diagram: not found, not frontend AuthZ |
| Concurrent start | service check + DB unique | partial unique index | database-design.md §7 / §9 |

---

# 14. Assumptions and Open Questions

**Assumptions (not new product rules):**

- Register does **not** create an HTTP session.
- Resume GET with no row returns **absence**, not an auto-created session.
- Other-user access is treated as **not found** (class-diagram).
- CSRF is configured for cookie auth but omitted from diagrams.
- Previous performance may be served by `WorkoutHistoryController` or nested in the session payload; both call `WorkoutHistoryService`.

| Question | Classification | Notes |
| --- | --- | --- |
| Auto-login immediately after register | NON-BLOCKING | Not required; §2 does not do it. HLD auth paragraph is combined signup/login wording. |
| Exact empty-resume representation | NON-BLOCKING | Absence vs explicit empty DTO is API design later |
| 404 vs 403 for other-user ids | NON-BLOCKING | Class-diagram prefers not-found; not a full error catalog |
| In-memory vs JDBC HTTP session store | DEFERRED | Outside application schema (ADR-006) |
| Collision UX copy | NON-BLOCKING | Product open; API conflict is specified |
| Default sets when adding exercise to empty session | NON-BLOCKING | Schema allows N rows; not this document |

No **BLOCKING** interaction questions. ADRs are not reopened.

---

# Final Self-Review

| Check | Result |
| --- | --- |
| class-diagram.md names | Controllers, services, repositories, `CurrentUser`, aggregates as specified |
| No JWT / access / refresh | Login uses HTTP-only session cookie only |
| Session states | `IN_PROGRESS` / `COMPLETED`; discard = delete |
| One active workout | Service check + partial unique index |
| History isolation | Snapshots at start; complete does not rewrite from catalog |
| Ownership | `CurrentUser` + owner-scoped repos |
| Previous performance | Current user + `exerciseId` + completed sets + snapshot name |
| No full API catalog | Flows only; 409/404 named only where architecture already did |
| No code/SQL | Diagrams and prose only |

**Ambiguity called out, not silently decided:** registration does not auto-authenticate; empty resume is “no active session” without a prescribed JSON shape.
