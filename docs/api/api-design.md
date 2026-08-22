# Workout Tracker — REST API Design (MVP)

**Status:** Practical REST contract for the approved MVP.  
**Not in this document:** Java controllers, DTO classes, OpenAPI artifacts, or executable code.

**Precedence:** `mvp-decisions.md` overrides `feature.md`. Endpoints follow `class-diagram.md`, `sequence-diagrams.md`, `database-design.md`, and ADR-001 through ADR-007.

**Base path:** `/api`  
**Versioning:** none for MVP (no `/v1`). Breaking changes later can introduce versioning when needed.

---

# 1. API Design Principles

| Convention | Choice |
| --- | --- |
| Resource naming | Plural nouns: `routines`, `exercises`, `sessions`, `history` |
| URLs | kebab-case paths not needed; resource names are single words. Nested children: `/sessions/current/exercises/{workoutExerciseId}/sets/{setId}` |
| HTTP methods | `GET` read, `POST` create or non-CRUD actions (`complete`, `archive`, `login`), `PUT` full routine replacement, `PATCH` partial set update, `DELETE` discard or delete |
| JSON | `camelCase` field names (TypeScript SPA) |
| Identifiers | UUID strings. Path ids are lookups, not proof of ownership |
| Auth | Stateful **HTTP-only**, **Secure** session cookie after login. `credentials: include`. **No JWT / access / refresh tokens in bodies or headers** |
| CSRF | Cookie-authenticated mutating requests must send the Spring CSRF header (implementation detail of ADR-006). Documented as required for POST/PUT/PATCH/DELETE; not a second auth token |
| Authorization | Server uses `CurrentUser.id()`. Other users’ resources: **404** (do not leak existence), per class-diagram |
| Validation | **400** with field errors for Bean Validation |
| Conflicts | **409** for duplicate email, second `IN_PROGRESS` session, complete with no completed set |
| Success | Direct JSON resource (or `204` with empty body). **No** generic `{ success, data }` envelope |
| Dates | ISO-8601 instant strings (`startedAt`, `completedAt`). Duration for completed sessions is **derived seconds** (`durationSeconds`), not a stored column |
| Units | Weights are kilograms only; field name `weightKg` |

**Active workout singleton:** a user has at most one `IN_PROGRESS` session. The API exposes it as `/api/sessions/current` so the client does not need to remember a session id for logging.

**Statuses in JSON:** `"IN_PROGRESS"` | `"COMPLETED"` only. Discard is not a status.

---

# 2. Authentication API

Register does **not** create a session (`sequence-diagrams.md` §2). Login sets the session cookie (`§3`).

### `POST /api/auth/register`

| | |
| --- | --- |
| Auth | Public |
| Purpose | Create account (display name, email, password) |

**Request:**

```json
{
  "displayName": "Vishal",
  "email": "vishal@example.com",
  "password": "secret-value"
}
```

**Success `201`:**

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "displayName": "Vishal",
  "email": "vishal@example.com"
}
```

No `Set-Cookie` for auth session. Client must login.

**Failures:** `400` validation; `409` email already registered.

### `POST /api/auth/login`

| | |
| --- | --- |
| Auth | Public |
| Purpose | Verify password; establish server-side session |

**Request:** same shape as login credentials:

```json
{
  "email": "vishal@example.com",
  "password": "secret-value"
}
```

**Success `200`:** same user JSON as register. Response includes **`Set-Cookie`** (`HttpOnly`, `Secure` in production, session id). Body contains **no** token fields.

**Failures:** `400` validation; `401` unknown email or wrong password (same message; do not distinguish).

### `POST /api/auth/logout`

| | |
| --- | --- |
| Auth | Authenticated (cookie) |
| Purpose | Invalidate server session |

**Request:** empty body. **Success `204`.** Cookie cleared / session invalid. Workout data in PostgreSQL unchanged.

**Failures:** `401` if already unauthenticated (acceptable; implementation may also `204` — **NON-BLOCKING**). Specified here: **401** if no valid session.

### `GET /api/auth/me`

| | |
| --- | --- |
| Auth | Authenticated |
| Purpose | Session status and greeting/display name for the shell |

**Success `200`:** same user JSON as above. **Failures:** `401`.

---

# 3. Exercise API

Picker list = **system exercises** plus the current user’s **non-archived** custom exercises. Archived customs are omitted from list/search. Other users’ customs never appear.

### `GET /api/exercises`

| | |
| --- | --- |
| Auth | Authenticated |
| Purpose | Browse/search/filter for Add Exercise and desktop library |

**Query (all optional):**

| Param | Meaning |
| --- | --- |
| `q` | Case-insensitive name contains (simple substring; not a search engine) |
| `muscleGroup` | Filter; match on **primary** muscle group for MVP (secondary match is unspecified product — see open questions) |

**Success `200`:**

```json
{
  "exercises": [
    {
      "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "name": "Bench Press",
      "primaryMuscleGroup": "Chest",
      "secondaryMuscleGroups": ["Triceps", "Shoulders"],
      "category": "Barbell",
      "source": "SYSTEM"
    },
    {
      "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
      "name": "Smith Machine Incline Press",
      "primaryMuscleGroup": "Chest",
      "secondaryMuscleGroups": [],
      "category": "Machine",
      "source": "CUSTOM"
    }
  ]
}
```

`source`: `SYSTEM` (`createdByUserId` null) or `CUSTOM` (owned by caller). No `archivedAt` in picker results.

### `POST /api/exercises`

Create **custom** exercise. Auth required.

**Request:**

```json
{
  "name": "Smith Machine Incline Press",
  "primaryMuscleGroup": "Chest",
  "secondaryMuscleGroups": ["Triceps"],
  "category": "Machine"
}
```

**Success `201`:** exercise object with `source: "CUSTOM"`.

**Failures:** `400` validation; `401`.

### `PUT /api/exercises/{exerciseId}`

Update **own custom** exercise (current catalog fields). Not a snapshot rewrite of history.

**Request:** same fields as create. **Success `200`:** updated exercise.

**Failures:** `400`; `401`; `404` if id is another user’s custom or unknown; **`403`** if id is a **system** exercise (resource is visible but not mutable).

### `POST /api/exercises/{exerciseId}/archive`

Hide/archive own custom. Not a hard delete.

**Request:** empty. **Success `204`.** Subsequent picker lists omit it. Existing routines may still reference the id (application does not auto-strip). History snapshots unchanged.

**Failures:** `401`; `404` other user / unknown; `403` system exercise.

There is **no** un-archive endpoint (product did not require it).

---

# 4. Workout Routine API

**Choice: (A) full routine payload on create/update**, not (B) nested per-slot endpoints.

**Why:** MVP edits the whole template (name, order, planned counts) on one screen. One `PUT` keeps uniqueness and positions consistent in a single transaction (`WorkoutRoutine` aggregate). Nested add/reorder APIs add round-trips without a product need.

### `GET /api/routines`

Auth. **Success `200`:**

```json
{
  "routines": [
    {
      "id": "...",
      "name": "Push Day",
      "description": null,
      "exerciseCount": 5,
      "updatedAt": "2026-08-21T10:00:00Z"
    }
  ]
}
```

List is summary-only (no full slot graph) for dashboard/routine index.

### `GET /api/routines/{routineId}`

Auth. **Success `200`:** full routine (below). **`404`** if missing or not owned.

### `POST /api/routines`

**Request / detail shape:**

```json
{
  "name": "Push Day",
  "description": "Chest and triceps",
  "exercises": [
    {
      "exerciseId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "plannedSetCount": 3
    }
  ]
}
```

Order in the array is `position` (1..n). Duplicate `exerciseId` → **`409`**. Non-pickable id → **`400`**. Empty `exercises` is allowed unless product later forbids it (not specified; **allowed**).

**Success `201`:** full routine including server-assigned slot ids and positions:

```json
{
  "id": "...",
  "name": "Push Day",
  "description": "Chest and triceps",
  "exercises": [
    {
      "id": "slot-uuid",
      "exerciseId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "exerciseName": "Bench Press",
      "plannedSetCount": 3,
      "position": 1
    }
  ]
}
```

`exerciseName` here is **live catalog name** (template, not history).

### `PUT /api/routines/{routineId}`

Same body as create. Replaces name, description, and **entire** exercise list. **Success `200`.** **`404`** not owned.

Does **not** modify any `workout_session`.

### `DELETE /api/routines/{routineId}`

**Success `204`.** Sessions that originated from it keep data; `originRoutineId` becomes null in persistence (not necessarily returned on history if null).

---

# 5. Active Workout Session API

All require auth. Mutations apply only to the caller’s `IN_PROGRESS` session.

### Session resource (response)

```json
{
  "id": "...",
  "name": "Push Day",
  "status": "IN_PROGRESS",
  "originRoutineId": "...",
  "startedAt": "2026-08-21T18:00:00Z",
  "completedAt": null,
  "durationSeconds": null,
  "exercises": [
    {
      "id": "we-uuid",
      "exerciseId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "exerciseName": "Bench Press",
      "position": 1,
      "sets": [
        {
          "id": "set-uuid",
          "setNumber": 1,
          "weightKg": 40,
          "repetitions": 12,
          "completed": true
        },
        {
          "id": "set-uuid-2",
          "setNumber": 2,
          "weightKg": null,
          "repetitions": null,
          "completed": false
        }
      ]
    }
  ]
}
```

`exerciseName` is the **snapshot**. `originRoutineId` may be null (empty start or routine later deleted). `durationSeconds` is null while `IN_PROGRESS`; set after complete as `completedAt - startedAt`.

### `POST /api/sessions`

Start workout. **One endpoint**, body discriminates:

**From routine:**

```json
{ "routineId": "..." }
```

**Empty:**

```json
{ "name": "Ad hoc" }
```

Exactly one of `routineId` or `name` must be present (`400` if both or neither).

**Success `201`:** session resource (`IN_PROGRESS`). From routine: copied name, exercises, planned empty sets. Empty: no exercises.

**Failures:** `400`; `404` routine not owned; **`409`** user already has `IN_PROGRESS`.

### `GET /api/sessions/current`

Resume. **Success `200`:** session resource. **`204`** no body if none — client may start (sequence-diagrams: absence, not auto-create).

### `POST /api/sessions/current/exercises`

Add exercise while `IN_PROGRESS`.

```json
{
  "exerciseId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "initialSetCount": 3
}
```

`initialSetCount` is required (`>= 1`) so the API does not invent a default planned count (routines have planned count; empty sessions do not).

**Success `200`:** full current session (simplest client refresh). Snapshot `exerciseName` taken at add time.

**Failures:** `400` not pickable / validation; `404` no current session; `409` if somehow not `IN_PROGRESS` (should not happen if current is only IN_PROGRESS).

Same `exerciseId` **may** appear twice on a session (routine uniqueness does not apply).

### `DELETE /api/sessions/current/exercises/{workoutExerciseId}`

Remove block (and its sets). **Success `200`:** updated session. **`404`** if no current session or id not in that session.

### `POST /api/sessions/current/exercises/{workoutExerciseId}/sets`

Add a set (append). **Success `200`:** session. Empty new set: `weightKg`/`repetitions` null, `completed` false.

### `PATCH /api/sessions/current/exercises/{workoutExerciseId}/sets/{setId}`

```json
{
  "weightKg": 50,
  "repetitions": 10,
  "completed": true
}
```

Fields optional except that `completed: true` requires weight and reps (400 if missing). **Success `200`:** session.

**Failures:** `400` negative values / completed without values; `404` not in current session.

### `DELETE /api/sessions/current/exercises/{workoutExerciseId}/sets/{setId}`

**Success `200`:** session. Application rewrites `setNumber` as needed.

### `POST /api/sessions/current/complete`

**Success `200`:** session with `status: "COMPLETED"`, `completedAt` set, `durationSeconds` populated. Incomplete sets may remain with `completed: false`; they are not completed workout data.

**Failures:** `404` no `IN_PROGRESS`; **`409`** zero completed sets.

After this, `/api/sessions/current` is `204`. History contains the completed id.

### `DELETE /api/sessions/current`

Discard: **physical delete** of the `IN_PROGRESS` aggregate. **Success `204`.** **`404`** if none.

No `DISCARDED` in JSON. Cannot restore.

There is **no** `PATCH` on `/api/history/{id}` and **no** set update by completed session id.

---

# 6. Previous Performance API

**Choice:** read-oriented resource on the **exercise**, scoped to the current user:

`GET /api/exercises/{exerciseId}/previous-performance`

**Why:** `WorkoutHistoryService.getPreviousPerformance(userId, exerciseId)` already takes user + stable exercise id. Nesting under `/sessions/current` would hide the call when building a routine, and a generic `/history/performance` query is equivalent but less obvious. Exercise id is the grouping key (decision 26).

Auth required. **Does not** return other users’ data.

**Success `200`:**

```json
{
  "exerciseId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "exerciseName": "Bench Press",
  "completedAt": "2026-08-14T19:00:00Z",
  "sessionId": "...",
  "sets": [
    { "setNumber": 1, "weightKg": 50, "repetitions": 10 }
  ]
}
```

`exerciseName` is the **historical snapshot** from that completed session, not the live catalog name. `sets` are **completed** sets only.

**`204`** if the user has no completed session containing that `exerciseId` (including first-time lifts). **`404`** only if the exercise id is not pickable **and** not in the user’s history — prefer **`204`** when the exercise is known/pickable but unused, and **`404`** if the id is another user’s custom with no history (do not leak). Simplest consistent rule: **`204` when no previous completed sets for this user and id; `404` only if `exerciseId` is not a UUID the caller is allowed to know as pickable and has no own history.** Even simpler for MVP: **`204` if no previous performance; never 404 for “no history”.** If `exerciseId` is garbage UUID, still `204` (no leak). **Chosen:** **`204` No Content** when there is no previous completed performance for this user and id. Do not 404 for empty history.

Archived customs: still `200` if history exists (identity remains).

---

# 7. Workout History API

Only `COMPLETED` sessions. Active workouts are **not** listed.

No pagination (product did not require it; reverse chronological full list is acceptable for MVP). No extra filters.

### `GET /api/history`

```json
{
  "workouts": [
    {
      "id": "...",
      "name": "Push Day",
      "completedAt": "2026-08-21T19:30:00Z",
      "durationSeconds": 5400,
      "exerciseCount": 5,
      "completedSetCount": 12
    }
  ]
}
```

`completedSetCount` counts `completed: true` only.

### `GET /api/history/{sessionId}`

**Success `200`:** same session resource shape as active, with `status: "COMPLETED"`, snapshots, all sets (client highlights completed). **`404`** if not owned, not `COMPLETED`, or discarded/never existed.

### `DELETE /api/history/{sessionId}`

Approved product/HLD: delete completed record. **Success `204`.** Cascades children. **`404`** if not a completed session owned by the user. Does not delete routines or exercises.

---

# 8. Request and Response Models

Shared **error** body (no envelope on success):

```json
{
  "status": 409,
  "code": "ACTIVE_SESSION_EXISTS",
  "message": "An in-progress workout already exists.",
  "fieldErrors": []
}
```

Validation `400`:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request is invalid.",
  "fieldErrors": [{ "field": "email", "message": "must not be blank" }]
}
```

**ID fields:** resource UUIDs (`id`, `exerciseId`, `routineId`, `originRoutineId`). No `userId` on resources (implied by the session). No `passwordHash`.

**Snapshots:** `exerciseName` on session/history/previous-performance. **Live names:** routine detail `exerciseName` from catalog.

**Mutable vs historical:** `/sessions/current*` mutates `IN_PROGRESS` only. `/history*` is read/delete of `COMPLETED` only.

**Dashboard (P0, composed + light aggregate):**

### `GET /api/dashboard`

Auth. Avoids extra round-trips for home. **Does not** include streak or “this week” (week undefined).

```json
{
  "displayName": "Vishal",
  "hasActiveSession": true,
  "activeSessionId": "...",
  "routines": [ { "id": "...", "name": "Push Day", "exerciseCount": 5 } ],
  "mostRecentCompleted": { "id": "...", "name": "Push Day", "completedAt": "..." },
  "totalCompletedWorkouts": 12,
  "totalCompletedSets": 140
}
```

`mostRecentCompleted` may be `null`. `hasActiveSession` true ⇒ client can `GET /sessions/current`. This is a **read model** over existing aggregates, not a new table.

---

# 9. HTTP Status and Error Design

| Status | When |
| --- | --- |
| **400** | Bean Validation; both/neither start fields; not pickable exercise on add; completed flag true without weight/reps; negative numbers |
| **401** | Missing/invalid session cookie |
| **403** | Mutate **system** exercise (visible catalog item) |
| **404** | Unknown or **other-user** routine/session/custom id; discard/complete/`current` mutations when no `IN_PROGRESS`; history id not completed/owned |
| **409** | Duplicate email; **second active workout**; **complete with zero completed sets**; duplicate `exerciseId` in a routine payload |
| **204** | Logout, archive, discard, delete history, no current session, no previous performance |
| **201** | Register, create custom, create routine, start session |
| **200** | Updates and GETs with a body |

**422** is **not** used; state conflicts are **409**, payload problems **400**.

| Situation | Status |
| --- | --- |
| Start second active workout | **409** `ACTIVE_SESSION_EXISTS` |
| Update another user’s resource | **404** |
| Update completed workout via current APIs | **404** (no `IN_PROGRESS`) |
| PATCH history | **405** Method Not Allowed (no such operation) |
| Edit system exercise | **403** |
| Duplicate exercise in routine | **409** `DUPLICATE_ROUTINE_EXERCISE` |
| Invalid set data | **400** |
| Complete with no completed set | **409** `NO_COMPLETED_SETS` |
| Login failure | **401** |

---

# 10. Active Workout State and Concurrency

- **One active workout:** `POST /api/sessions` fails **409** if `GET /api/sessions/current` would be **200**.
- **Resume:** `GET /api/sessions/current` → **200** or **204**.
- **Two simultaneous starts:** application check then PostgreSQL partial unique index. Loser → **409**. No retry, no distributed lock.
- **Client:** on 409, `GET /api/sessions/current` (resume) or `DELETE` (discard) then retry start. UX copy is unspecified.

Database remains the last integrity guarantee.

---

# 11. Endpoint Summary

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | No | Create account |
| POST | `/api/auth/login` | No | Session cookie + user |
| POST | `/api/auth/logout` | Yes | Invalidate session |
| GET | `/api/auth/me` | Yes | Current user |
| GET | `/api/dashboard` | Yes | Home summary (no streak/week) |
| GET | `/api/exercises` | Yes | Picker / library |
| POST | `/api/exercises` | Yes | Create custom |
| PUT | `/api/exercises/{exerciseId}` | Yes | Update own custom |
| POST | `/api/exercises/{exerciseId}/archive` | Yes | Hide custom |
| GET | `/api/exercises/{exerciseId}/previous-performance` | Yes | Latest completed sets for exercise |
| GET | `/api/routines` | Yes | List templates |
| GET | `/api/routines/{routineId}` | Yes | Template detail |
| POST | `/api/routines` | Yes | Create template |
| PUT | `/api/routines/{routineId}` | Yes | Replace template |
| DELETE | `/api/routines/{routineId}` | Yes | Delete template |
| POST | `/api/sessions` | Yes | Start routine or empty |
| GET | `/api/sessions/current` | Yes | Resume |
| POST | `/api/sessions/current/exercises` | Yes | Add exercise |
| DELETE | `/api/sessions/current/exercises/{workoutExerciseId}` | Yes | Remove exercise |
| POST | `/api/sessions/current/exercises/{workoutExerciseId}/sets` | Yes | Add set |
| PATCH | `/api/sessions/current/exercises/{workoutExerciseId}/sets/{setId}` | Yes | Update set |
| DELETE | `/api/sessions/current/exercises/{workoutExerciseId}/sets/{setId}` | Yes | Remove set |
| POST | `/api/sessions/current/complete` | Yes | Complete |
| DELETE | `/api/sessions/current` | Yes | Discard (hard delete) |
| GET | `/api/history` | Yes | Completed list |
| GET | `/api/history/{sessionId}` | Yes | Completed detail |
| DELETE | `/api/history/{sessionId}` | Yes | Delete completed record |

---

# 12. Mapping to Backend Architecture

| Endpoint group | Controller | Service | Aggregate | Persistence |
| --- | --- | --- | --- | --- |
| `/api/auth/*` | `AuthController` | `AuthService` | `User` | `UserRepository` / `app_user` |
| `/api/dashboard` | thin dashboard on `WorkoutHistoryController` or `AuthController` composing services | `WorkoutHistoryService` + `WorkoutRoutineService` + `WorkoutSessionService.getInProgress` | reads only | session + routine queries |
| `/api/exercises` CRUD/archive | `ExerciseController` | `ExerciseService` | `Exercise` | `exercise` |
| `/api/exercises/.../previous-performance` | `WorkoutHistoryController` **or** `ExerciseController` delegating | `WorkoutHistoryService` | `WorkoutSession` graph | `workout_session` / children |
| `/api/routines` | `WorkoutRoutineController` | `WorkoutRoutineService` | `WorkoutRoutine` | `workout_routine`, `routine_exercise` |
| `/api/sessions*` | `WorkoutSessionController` | `WorkoutSessionService` | `WorkoutSession` | `workout_session` cascade |
| `/api/history` | `WorkoutHistoryController` | `WorkoutHistoryService` | `WorkoutSession` (`COMPLETED`) | same tables |

Prefer previous-performance routed through `WorkoutHistoryController` mapped at `/api/exercises/{id}/previous-performance` (Spring mapping can live on history controller). Do not add an `Exercise` write in that handler.

---

# 13. Security Review

| Check | Result |
| --- | --- |
| Session cookie auth | Login `Set-Cookie` only; later calls cookie + CSRF on mutations |
| No JWT / token JSON | Register/login/me bodies have id, displayName, email only |
| Ownership | Path ids + `CurrentUser`; 404 cross-user |
| Sensitive fields | No `passwordHash`, no session store internals |
| Completed immutable | No PATCH on history; current APIs require `IN_PROGRESS` |
| System exercises | PUT/archive → 403 |
| Discard | DELETE current only if `IN_PROGRESS`; cascade children |
| Picker | No other-user customs; no archived customs |

---

# 14. Open Questions

| Question | Classification | Notes |
| --- | --- | --- |
| CSRF cookie/header names | NON-BLOCKING | Spring default vs custom; required for cookie auth |
| Logout `401` vs `204` if already logged out | NON-BLOCKING | §2 specifies 401 |
| `muscleGroup` matching secondary | NON-BLOCKING | API filters primary only until product says otherwise |
| History list pagination | DEFERRED | Not in product; add later if lists grow |
| Dashboard as separate resource vs client composition | NON-BLOCKING | Included as `GET /api/dashboard` for P0 home; could be omitted if SPA calls me + routines + history |
| Empty routine (`exercises: []`) | NON-BLOCKING | Allowed here |
| `GET /exercises/{id}` single resource | NON-BLOCKING | Not required; picker list + ids on routines suffice |

No **BLOCKING** items. No JWT, week-stats, streak, rest timer, or Settings endpoints.

---

# Final Self-Review

| Check | Result |
| --- | --- |
| P0 paths | Auth, dashboard, exercises, routines, start/resume/log/complete/discard, previous performance, history view/delete |
| No unapproved features | No reset, OAuth, PRs, charts, notes, skip, lbs, Settings |
| Matches sequences | Register without session; login cookie; 409 second start; discard delete; previous performance user+exerciseId |
| Maps to class-diagram | Controllers/services as §12 |
| DB constraints | Unique email, one IN_PROGRESS, unique exercise per routine, cascade discard |
| States | IN_PROGRESS / COMPLETED only |
| Discard | DELETE `/sessions/current` |
| Auth | Cookie only |
| Ownership | Server-side 404/403 |
| History | GET/DELETE completed; no PATCH |
| MVP size | Singleton current session; full routine PUT; no pagination/versioning |

**Ambiguity handled explicitly:** no auto-login on register; `204` for no current session and no previous performance; week/streak omitted from dashboard.
