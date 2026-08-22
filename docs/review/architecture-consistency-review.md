# Workout Tracker — Architecture Consistency Review (MVP)

**Type:** Review only. No new product features, schema, or APIs.  
**Date context:** Documents as of this review.  
**Does not rewrite** accepted ADRs or `mvp-decisions.md`.

---

# 1. Review Scope

**Artifacts reviewed**

| Layer | Documents |
| --- | --- |
| Product | `docs/product/feature.md`, `product-review.md`, `mvp-decisions.md`, `mvp-spec-readiness.md` |
| Architecture | `high-level-design.md`, `architecture-decisions.md`, `class-diagram.md`, `sequence-diagrams.md` |
| Database | `docs/database/database-design.md` |
| API | `docs/api/api-design.md` |
| Rules | `.cursor/rules/` (`00`–`05`) |

**Precedence used (as specified):**

1. `mvp-decisions.md` — product  
2. `architecture-decisions.md` — architecture  
3. `mvp-spec-readiness.md` — readiness / unresolved product questions  
4. `high-level-design.md` — system structure  
5. `database-design.md` — persistence  
6. `class-diagram.md` — modules/classes  
7. `sequence-diagrams.md` — flows  
8. `api-design.md` — REST contract  

Where a **lower** document contradicts an **accepted** decision (1 or 2), this review **reports the conflict**. Implementation must follow the higher document, not invent a third design.

This review checks **mutual consistency** and **whether implementation planning can start**. It does not add capabilities.

---

# 2. Executive Verdict

**READY WITH NON-BLOCKING ISSUES**

The write model, auth mechanism, session lifecycle, history snapshots, and REST surface are aligned with `mvp-decisions.md` and **ADR-001–007**, and they are implemented consistently in the database, class, sequence, and API documents.

The **High-Level Design is stale** in a few sections (persisted `DISCARDED`, cookie-vs-JWT still “open”). Those sections **lose** to ADR-006 and ADR-007. That is documentation drift, not an open product question. If implementers follow HLD §8 / §14 / §15 instead of the ADRs, they would build the wrong lifecycle or auth. The implementation plan must **cite ADRs as binding** and treat those HLD passages as superseded.

Remaining gaps (CSRF cookie names, HTTP session store in-memory vs JDBC, dashboard controller owner) are **implementation notes**, not missing P0 capabilities.

---

# 3. Product Capability Traceability

P0/MVP capabilities from `mvp-decisions.md` and the readiness P0 row. `feature.md` narrative that was deferred is **not** required.

| Approved MVP capability | HLD | Database | Class diagram | Sequence | API | Status |
| --- | --- | --- | --- | --- | --- | --- |
| Register (display name, email, password; no auto-login) | §7.1 combined wording | `app_user` | `AuthService.register` | §2 no session | `POST /api/auth/register` | **OK** (HLD paragraph mixes signup/login; sequences/API are authoritative for no auto-login) |
| Login + logout | §7.1, ADR-006 | user lookup only | `AuthService.login/logout` | §3 | `POST /api/auth/login`, `/logout` | **OK** |
| Current user | implied | — | `GET` via `me` | cookie after login | `GET /api/auth/me` | **OK** |
| Private per-user data | §10 | owner FKs | `findByIdAndUserId` | §11 | 404 cross-user | **OK** |
| Dashboard (no streak; no week until defined) | §1, §6 | no week column | totals on history service; no Dashboard module | not a dedicated sequence | `GET /api/dashboard` (no streak/week) | **OK** with note: dedicated dashboard URL is an API convenience; data exists without a new table |
| Routine CRUD, order, planned set count | §5 workout | `workout_routine`, `routine_exercise` | `WorkoutRoutineService` | start uses routine | `/api/routines` PUT full payload | **OK** |
| Duplicate exercise forbidden on routine | decision 21 | unique `(routine, exercise)` | service + DB | — | **409** | **OK** |
| Global catalog | §5 exercise | `exercise` system rows | `ExerciseService.listPickable` | start `requirePickable` | `GET /api/exercises` `source: SYSTEM` | **OK** (seed **content** not specified — non-blocking) |
| Custom create/edit | decision 17 | custom + owner | `createCustom` / `updateCustom` | — | `POST`/`PUT /api/exercises` | **OK** |
| Archive/hide custom | decision 17 | `archived_at` | `archiveCustom` | — | `POST .../archive` | **OK** |
| System exercises immutable to users | DB + class | check cannot archive system | `assertMutableBy` | — | **403** on PUT/archive system | **OK** |
| Start from routine + snapshots | §7.3, ADR-005 | session copy + `exercise_name` | `startFromRoutine` | §4 | `POST /api/sessions` `{ routineId }` | **OK** |
| Start empty | §7.3 | session, no origin | `startEmpty` | §5 | `{ name }` | **OK** |
| One `IN_PROGRESS` per user | §8, DB partial unique | partial unique index | 409 pre-check | §4, §12 | **409** | **OK** |
| Resume | §7.5 | SELECT `IN_PROGRESS` | `getInProgress` | §6 absence if none | `GET /api/sessions/current` **200/204** | **OK** (204 is API’s encoding of “absence”) |
| Add/remove session exercises | §7.4 | cascade children | session aggregate | implied with set flow | nested `/sessions/current/exercises` | **OK** |
| Add/update/remove sets; mark completed | §7.4 | `workout_set` | mutators + `assertInProgress` | §7 | PATCH/POST/DELETE sets | **OK** |
| Previous performance (latest completed, same exercise, this user) | §7.8 | query + snapshot name | `WorkoutHistoryService` | §10 | `GET /api/exercises/{id}/previous-performance` | **OK** |
| Complete (≥1 completed set) → `COMPLETED` | §7.6 | status + `completed_at` | `complete()` | §8 | `POST .../complete` **409** if none | **OK** |
| Discard = physical delete of `IN_PROGRESS` | **HLD stale**; **ADR-007** | CASCADE delete | `discard` → `delete` | §9 | `DELETE /api/sessions/current` | **OK in DB/class/seq/API**; **HLD §8/§14 conflict** (see §4) |
| History list/detail | §7.7 | `COMPLETED` only | `WorkoutHistoryService` | — | `GET /api/history` | **OK** |
| Delete completed history | product + HLD | cascade session | `deleteCompleted` | — | `DELETE /api/history/{id}` | **OK** |
| Historical snapshot (name at add) | §9 | `exercise_name` | `WorkoutExercise.exerciseName` | §4 note | session/history JSON `exerciseName` | **OK** |
| Online-only | decision 22 | no sync tables | no offline types | — | no offline APIs | **OK** |
| kg only | decision 7 | `weight_kg` | `weightKg` | — | `weightKg` | **OK** |
| Duration from timestamps | decision 25 | no duration column | derived | complete flow | `durationSeconds` | **OK** |

**Not in this table (deferred / out of scope):** rest timer, streak, charts, PRs, Settings, password reset, email verification, duplicate routine, Progress **screen**, account deletion, offline.

---

# 4. Product Decision Consistency

| Decision | Reflected in DB / class / seq / API? | Notes |
| --- | --- | --- |
| Persisted states `IN_PROGRESS` / `COMPLETED` only | Yes (except HLD) | **Conflict:** HLD §8 state diagram and §14 row still include **`DISCARDED` as a lifecycle state** and treat hard-delete as optional. **ADR-007 wins.** |
| Discard = physical delete | Yes below HLD | ADR-007, DB §7/§11, class, seq §9, API DELETE current |
| One active workout | Yes | Service 409 + partial unique index |
| Completed immutable except history delete | Yes | No PATCH history; current APIs require `IN_PROGRESS` |
| Routine change/delete ≠ rewrite history | Yes | Copy at start; `ON DELETE SET NULL` origin |
| Exercise rename ≠ snapshot | Yes | `exercise_name` at add time |
| Previous performance = latest **completed** session, same identity, **current user** | Yes | History service + `/previous-performance` |
| Unique exercise per **routine** not per session | Yes | DB unique on routine only; API allows duplicate `exerciseId` on a session |
| Custom ownership / system immutable | Yes | |
| Online-only | Yes | |
| No Settings / rest timer / streak / charts / PRs / JWT / reset | Yes in API and later artifacts | `feature.md` still describes them; **do not implement from feature.md** |

**HLD §15** still lists “Cookie vs JWT” and “Discard = status vs hard-delete” as open. **Superseded** by ADR-006 (accepted cookie session) and ADR-007 (accepted physical delete).

---

# 5. Authentication and Security Consistency

| Topic | Source of truth | Later artifacts | Consistent? |
| --- | --- | --- | --- |
| Server-side session + HttpOnly + Secure cookie | ADR-006 | seq §3, class `SecurityConfig`, API login `Set-Cookie` | **Yes** |
| No JWT / access / refresh | ADR-006 | All later docs | **Yes** |
| Password hash never in API | DB + class + API | User DTO: id, displayName, email | **Yes** |
| Ownership server-side; frontend not trusted | HLD §10, class §10 | seq §11, API 404 | **Yes** |
| Cross-user | 404 (class, API) | seq “Not found” | **Yes** |
| System exercise mutate | 403 API; class 403/404 | **Aligned enough** (API picked 403) | **Yes** |
| Register without session | seq §2, API | HLD §7.1 is ambiguous | **Yes** if seq/API followed |

**Must resolve before coding (not new product):**

| Item | Severity | Why |
| --- | --- | --- |
| CSRF header/cookie pairing for cross-origin SPA | **IMPLEMENTATION NOTE** | ADR-006 requires CSRF; API says required; exact Spring cookie/header names not in ADRs |
| Servlet session store: memory vs Spring Session JDBC | **IMPLEMENTATION NOTE** | ADR-006 allows either; not application schema. Single-instance MVP can start in-memory |
| CORS + `SameSite` for split SPA/API origins | **IMPLEMENTATION NOTE** | Called out in ADR-006 risks; deployment-specific |

None of these reopen JWT.

---

# 6. Database-to-Domain Consistency

Seven tables map 1:1 to `User`, `Exercise`, `WorkoutRoutine`, `RoutineExercise`, `WorkoutSession`, `WorkoutExercise`, `WorkoutSet`.

| Concern | Finding |
| --- | --- |
| Aggregates | Routine owns slots; session owns exercises/sets; exercise independent — matches class §5 |
| Ownership columns | `user_id` / `created_by_user_id` — matches services |
| Ordering / uniqueness | `position`, unique exercise per routine — matches |
| Lifecycle | CHECK status; partial unique `IN_PROGRESS` — matches |
| Snapshot | `workout_exercise.exercise_name` — matches domain field |
| Previous performance | `exercise_id` + `COMPLETED` + `is_completed` — indexed |
| Discard cascade | session → exercise → set **CASCADE**; routine → session **SET NULL** | **Correct** |
| History delete | same cascade on completed session | **Correct** |
| Archive | `archived_at`; no hard delete of exercise | **Correct** |
| Duration | not stored; API derives | **Correct** |
| Auth tables | no JWT/session tables in app schema | **Correct** |

**No missing P0 field or contradictory FK.** No new schema proposed.

**Unnecessary duplication:** none beyond the **intended** name snapshot.

---

# 7. Class Diagram and Sequence Consistency

| Sequence | Participants map to class-diagram? |
| --- | --- |
| Registration | `AuthController` → `AuthService` → `UserRepository` → PostgreSQL |
| Login | + Spring Security boundary / session cookie (config, not a domain class) |
| Start from routine | `WorkoutSessionService`, `WorkoutRoutineRepository`, `ExerciseService`, `WorkoutSession` |
| Start empty | same without routine |
| Resume | `getInProgress` + repository graph load |
| Update set | `assertInProgress` + `WorkoutSet` |
| Complete | `WorkoutSession.complete` |
| Discard | `delete`, not a status type |
| Previous performance | `WorkoutHistoryService` + `WorkoutSessionRepository` |
| Cross-user | `CurrentUser` + `findByIdAndUserId` |
| Concurrent start | service + DB unique — no extra class |

**No sequence participant lacks a class/service/repository** (Spring Security is the integration boundary, as designed).

---

# 8. API-to-Architecture Consistency

| Endpoint group | Backend owner | Aggregate | Persistence | Issue? |
| --- | --- | --- | --- | --- |
| `/api/auth/*` | `AuthController` / `AuthService` | `User` | `app_user` | None |
| `/api/dashboard` | API says History **or** Auth composing services | read model | queries | **IMPLEMENTATION NOTE:** pick one controller in the implementation plan; class-diagram forbade a Dashboard **module**, not a GET |
| `/api/exercises` CRUD/archive | `ExerciseController` / `ExerciseService` | `Exercise` | `exercise` | None |
| `/api/exercises/{id}/previous-performance` | `WorkoutHistoryController` (path on exercise) | `WorkoutSession` read | completed graph | **OK** — does not write `Exercise`; class-diagram allowed this mapping |
| `/api/routines` | `WorkoutRoutineController` | `WorkoutRoutine` | routine tables | Full PUT respects aggregate |
| `/api/sessions*` | `WorkoutSessionController` | `WorkoutSession` | session cascade | Nested set/exercise mutations stay on session aggregate |
| `/api/history` | `WorkoutHistoryController` | same session, `COMPLETED` | same | None |

No endpoint requires unsupported persistence (e.g. `DISCARDED` row, token table).

**Not duplicate:** `GET /dashboard` vs `GET /auth/me` + lists — dashboard is optional aggregation; not a second write model.

---

# 9. API Lifecycle and State Consistency

| Rule | API | Sequences / product | Consistent? |
| --- | --- | --- | --- |
| Active mutations only `IN_PROGRESS` | `/sessions/current*` | seq §7 | **Yes**; no current → **404** on mutations, **204** on GET |
| History = `COMPLETED` | `/api/history` | — | **Yes** |
| Complete transition | `POST .../complete` | seq §8 | **Yes** |
| Discard delete | `DELETE .../current` | ADR-007, seq §9 | **Yes**; no `DISCARDED` JSON |
| History not editable | no PATCH | class | **Yes**; DELETE allowed |
| Second start | **409** | seq, DB | **Yes** |
| GET current empty | **204** | seq “absence” | **Yes** (status code is API-level; product did not specify 204 vs 404) |
| Previous performance empty | **204** | seq empty | **Yes** |

API §6 briefly discusses 404 vs 204 then **chooses 204** for no previous performance. Not a conflict with product.

---

# 10. Concurrency and Integrity Review

| Topic | Classification | Comment |
| --- | --- | --- |
| Application `findInProgress` then insert | **IMPLEMENTATION NOTE** | Needed for 409 message; race still possible |
| Partial unique index | **READY** | Final guarantee |
| Concurrent start → 409 | **READY** | seq §12 + API §10; no retry required |
| Duplicate routine exercise | **READY** | 409 + DB unique |
| Ownership | **READY** | Owner in query |
| Transactions | **IMPLEMENTATION NOTE** | Class-diagram `@Transactional` on session/routine writes; not specified per-endpoint in API (normal Spring practice) |
| Distributed locks / queues | **Not used** | Correct for MVP |

No **BLOCKING** integrity gap.

---

# 11. Deferred Features and Scope Creep Review

| Feature | Accidentally in later artifacts? |
| --- | --- |
| JWT / refresh / OAuth / reset / verify | **No** |
| Rest timer / streak / charts / PRs / analytics / subscriptions | **No** APIs |
| Microservices / Kafka / GraphQL / extra DB | **No** |
| Settings / Progress screen | **No** |
| `GET /api/dashboard` | **Justified** P0 home read model; omits week/streak. Not a new product feature. |
| `initialSetCount` on add-exercise-to-session | **Justified** so API does not invent a default of 3; product unspecified count for empty-session add |

`feature.md` still contains deferred screens; **scope for implementation is mvp-decisions + later ADRs**, not raw `feature.md`.

---

# 12. Issues and Required Actions

| ID | Severity | Artifact(s) | Issue | Why it matters | Recommended action |
| --- | --- | --- | --- | --- | --- |
| C-01 | **NON-BLOCKING** | HLD §8, §14, §15 vs ADR-006, ADR-007 | HLD still shows persisted/`DISCARDED` state, JWT-as-alternative, discard status vs delete as open | Implementers reading HLD alone may add a `DISCARDED` column or JWT | Implementation plan: **ADRs override HLD** for auth and discard. Optional later HLD errata (out of this review’s redesign mandate) |
| C-02 | **NON-BLOCKING** | HLD §7.1 vs seq §2 / API register | Combined “signup then session” wording | Could imply auto-login | Follow sequence/API: register **201** without cookie |
| C-03 | **IMPLEMENTATION NOTE** | ADR-006, API §1, API §14 | CSRF and CORS/`SameSite` not named to a Spring property set | Cookie SPA will fail mutations if skipped | Specify in implementation plan (e.g. cookie CSRF + allowed origin) |
| C-04 | **IMPLEMENTATION NOTE** | ADR-006, DB §1 | HTTP session store (memory vs JDBC) | Multi-instance vs single process | Start in-memory for local/single instance; JDBC only if scaling |
| C-05 | **IMPLEMENTATION NOTE** | API §12 vs class §6 | Dashboard GET has two possible controllers | Confusion, not missing behavior | Assign `WorkoutHistoryController` (or a thin `DashboardController` in `workout.history` **package**, not a new module) |
| C-06 | **IMPLEMENTATION NOTE** | API §6 | Verbose 204 vs 404 discussion then 204 | Readers might think 404 is still in play | Implement **204** for no previous performance as the last sentence of API §6 |
| C-07 | — | — | Missing P0 mapping, JWT in API, DISCARDED in DB/API | — | **None found** in DB, class, sequence, or API |

No **BLOCKING** row.

---

# 13. Implementation Readiness Checklist

| Area | Mark |
| --- | --- |
| Product requirements (P0 via mvp-decisions) | **READY** (deferred items stay deferred) |
| Architecture decisions ADR-001–007 | **READY** |
| Authentication (cookie session) | **READY** (C-03, C-04 notes) |
| Database logical model | **READY** |
| Domain / class design | **READY** |
| Interaction flows | **READY** |
| API contract | **READY** (C-05, C-06 notes) |
| Concurrency / integrity | **READY** |
| Unresolved product questions (week, Progress page, collision UX copy, previous-set highlighting, muscle secondary filter) | **DEFERRED** / **NEEDS DECISION** only if those UIs are built; **not required to start P0 logging** |
| HLD vs ADR discard/auth text | **NEEDS FIX** of **docs** (optional) or **explicit ignore** in implementation plan (**C-01**) — not a schema rewrite |

---

# 14. Final Verdict and Next Step

**Implementation planning can begin.**

Bind the plan to:

- Product: `mvp-decisions.md` (not unimplemented `feature.md` sections)  
- Architecture: **ADR-006, ADR-007**, then class / sequence / API / database  
- Treat HLD as structure and module map; **do not** implement HLD §8 `DISCARDED` persistence or HLD §15 JWT option  

**Next artifact:** [`docs/implementation/implementation-plan.md`](../implementation/implementation-plan.md)

**Minimum before planning:** none required. **Minimum while planning:** state C-01 (ADR over HLD) so coding does not reintroduce `DISCARDED` or JWT.

---

# Final Self-Review

- No new product requirements or architecture redesign.  
- Issues cite real drift (HLD vs ADRs) or implementation-critical ops (CSRF, session store), not “nice to have” enterprise features.  
- Blockers vs notes are separated; none blocking.  
- Modular monolith, cookie session, physical discard, PostgreSQL-only, and DDD-style aggregates are preserved.  
- Review is usable by a solo implementer: follow ADRs + DB + class + API.
