# Specification Readiness Assessment

**Reviewed:** `docs/product/feature.md`, `docs/product/product-review.md`, `docs/product/mvp-decisions.md`, `.cursor/rules/`  
**Precedence applied:** Where `mvp-decisions.md` conflicts with `feature.md`, `mvp-decisions.md` is the approved clarification. This assessment does not choose among remaining gaps by inventing product rules. Leftover text in `feature.md` that contradicts a decision is treated as **unupdated narrative**, not as a second source of truth.

---

## 1. Overall Assessment

The product specification is **ready to start High-Level Design and conceptual database/API design** for the MVP slice defined in `mvp-decisions.md`.

The blocking gaps called out in `product-review.md` (session lifecycle, snapshots, kg-only, planned set count, P0 vs P1, Settings, skip vs remove, complete gate, one in-progress session, persist/resume/discard, custom exercise archive, online-only) are **closed by approved decisions**.

What remains is:

- **Document drift:** `feature.md` still describes rest timer, streak, Settings, duplicate routine, charts, and PRs as if they were MVP screens or P0.
- **Non-blocking UX and presentation gaps** listed in `mvp-decisions.md` § Remaining Non-Blocking Questions.
- **One unresolved product surface:** whether a dedicated Progress destination exists without charts/PRs. That does **not** prevent designing auth, catalog, routines, session lifecycle, history, or previous-performance lookup.

No remaining issue requires a new product decision before architects can name modules, session states, ownership boundaries, snapshot obligations, or the core relational entities. Gaps that would change **query semantics** (dashboard “this week”, Progress table “best weight / best reps”) affect a small number of read models, not the write model for logging.

---

## 2. Scope Consistency

### After applying precedence

| Area | Consistent MVP rule |
| --- | --- |
| P0 | Auth (signup / login / logout, display name required), dashboard (no streak, no Settings), routines (CRUD, reorder, planned set count, unique exercise per routine, no duplicate-routine), global catalog + custom create/edit/archive, start routine or empty, persist one in-progress session, log kg/reps/sets, previous performance, complete with ≥1 completed set, history view/delete, name snapshot, responsive online-only UI |
| P1 | Rest timer, charts, personal records, streak, duplicate routine (`mvp-decisions.md`) |
| P2 | Unchanged from `feature.md` §22 (notes, volume, export, dark mode, etc.); not re-opened |
| Out of scope | `feature.md` §21 plus account deletion, offline-first sync, password reset, email verification, Settings screen, lbs, target weight/reps, skip-as-distinct-from-remove |

### Remaining scope inconsistencies (do not re-open decisions)

These are **not** two equally valid product rules. They are `feature.md` not yet aligned with `mvp-decisions.md`.

- §10, §18, Flow 2 still include rest timer → **P1**.
- §5 / §16 still include streak on the dashboard → **P1**.
- §6 still lists duplicate routine as a requirement → **P1**.
- §19 still lists Settings → **no Settings screen**.
- §14 / §17 / §18 / Flow 3 still include charts and PRs → **P1**.
- §22 still lists previous performance as P1 → **P0**.
- §22 lists “basic statistics” as P1, while `mvp-decisions.md` keeps dashboard statistics **except streak** in MVP. Precedence: dashboard stats minus streak are in scope; streak is not.
- §9 still lists skip and exercise notes; skip is **remove-only**; notes remain **P2** per `feature.md` §22 and the decisions document (P2 not re-opened).
- §21 vs §22 still both mention supersets/RPE → treat as **out of scope / P2 leftover**; do not design them.

### Terminology

`mvp-decisions.md` glossary (**Exercise / Routine / Session**) is sufficient for HLD. `feature.md` “Workouts” navigation remains ambiguous for **UI labels only**. Specs and design should use the glossary; they must not introduce a fourth entity.

---

## 3. Architecture Readiness

**Ready.** A modular monolith HLD can be written without guessing product behavior that would change module boundaries.

Enough is specified to define:

- **Auth:** email/password, required display name, private per-user data, no reset/verification/account deletion in this slice.
- **Exercise:** global curated catalog vs user-owned custom; browse/search/filter; archive hides from future selection.
- **Routine:** user-owned templates; planned set count; one occurrence of an exercise per routine; delete allowed independently of history.
- **Session (workout module):** start from routine or empty; at most one in-progress per user; server-persisted resume; discard ≠ history; complete gate; previous performance from latest completed session containing the same exercise; duration of a **completed** session = `completedAt - startedAt`.
- **History:** completed sessions view/delete, no edit; snapshots so routine/catalog changes do not rewrite completed logs.
- **Previous performance:** a read of completed session data grouped by stable exercise identity when available—not a separate P1 analytics product.

Online-only and one persisted in-progress session imply **server as source of truth** for the live session (not an offline sync architecture). That follows the decisions; it is not a new requirement.

**Not required to invent before HLD:** Settings module, rest-timer service, PR engine, streak job, chart pipeline, duplicate-routine use case, offline queue.

**HLD should explicitly stay silent or “deferred” on:** dedicated Progress screen/module beyond the previous-performance read (see Remaining Issues). Do not add a Progress bounded context as if `feature.md` §18 were still unmodified, and do not delete previous-performance from the session experience.

---

## 4. Database Design Readiness

**Ready** for conceptual / logical data modeling (entities, ownership, lifecycle flags, snapshot of exercise **name**, planned set count, session status, completed-set flag). Physical keys, indexes, and SQL are out of scope for this assessment.

Enough is specified to model:

| Concept | Product basis |
| --- | --- |
| User with display name, email, credentials (credentials not a product API field) | `feature.md` §4, decision 24; `product-review.md` on `passwordHash` |
| Global exercises vs custom exercises owned by a user | decisions 17, 20 |
| Hidden/archived custom exercises still identifiable | decision 17; grouping decision 26 |
| Routine + ordered routine exercises + planned set count; uniqueness of exercise within a routine | decisions 8, 21 |
| Session owned by user; optional origin routine; in-progress vs completed vs discarded (discard is not history) | decisions 2–5, 16, 27 |
| At most one in-progress session per user | decision 2 |
| Session exercises with **snapshotted name** and **identity when available** | decisions 26, 30, 27 |
| Sets with completion status; only completed sets are completed workout data | decision 6; `feature.md` set completion |
| kg as the only logged unit | decision 7 |
| Duration not an independent product fact for completed sessions | decision 25 |

**Database design can proceed without:** week-boundary columns, PR tables, streak tables, rest-timer preferences, unit preference, Settings, notes fields (P2), target weight/reps, skip status.

**Must not encode as if decided:** unique exercise per **session** (only routines are constrained); dropping vs keeping incomplete rows after complete (same set records with a completion flag satisfy decision 6 either way); a numeric default for planned set count (the field is required by the routine rule, not a default of N).

Project rule “completed data remains historically accurate when routines change” is aligned with decisions 27 and 30. Logical design must keep completed session payload independent of later routine edits.

---

## 5. API Design Readiness

**Ready** for resource-oriented API design of the MVP write/read paths, with a small set of response contracts left loosely specified (presentation, not resources).

Identifiable API surfaces from the spec:

- Signup / login / logout (no reset, no verify, no account delete).
- Dashboard: routines list, most recent completed session, start (routine or empty), stats excluding streak.
- Routine CRUD, reorder, planned set count; reject duplicate exercise on the same routine; delete routine without deleting history.
- Exercise list (global + own custom), search, muscle-group filter, create/edit custom, archive custom.
- Session: start, get in-progress, patch logging, add/remove exercise, add/remove sets, mark set completed, previous performance, discard, complete (409/reject if zero completed sets or if a second in-progress start is attempted).
- History list/detail/delete; no PATCH of completed sessions.

**API design does not need:** Settings, rest timer, PR, charts, streak, duplicate-routine, notes, volume, skip, lbs.

**Contracts that can be designed without a further product decision:**

- Previous performance: the **latest completed session that contains the exercise**, **excluding incomplete sets** (decisions 6, 18). Which **subset of completed sets** is emphasized in the UI is presentation; the API can expose that session’s completed sets for the exercise without inventing a “best set” rule.
- In-progress collision: start is not allowed when one exists (decision 2). Prompt copy is UI.
- Live duration before complete: client can render elapsed time from `startedAt`; product duration is defined only after complete (decision 25 + `feature.md` §9 timer as display).

**Contracts that remain underspecified but do not block the rest of the API:** dashboard “this week”; dedicated Progress collection vs active-session previous performance only; muscle filter matching primary vs secondary; decimal/zero weight validation messages.

---

## 6. Remaining Issues

| Issue | Source Documents | Classification | Impact | Recommendation |
| --- | --- | --- | --- | --- |
| `feature.md` still lists rest timer, streak, Settings, duplicate routine, charts, PRs, previous performance as P1 vs P0, skip, notes on active workout | `feature.md` §§5–10, 14–19, 22–23; `mvp-decisions.md` conflicts table | NON-BLOCKING | HLD/API might copy stale screens if `feature.md` is read alone | Follow `mvp-decisions.md`. Treat leftover `feature.md` text as unupdated. Do not design those capabilities in the MVP HLD. |
| Previous performance: which completed set(s) to **highlight** in UI | `mvp-decisions.md` Q1; `feature.md` §9 example line | NON-BLOCKING | UI copy; optional response shaping | Decision 18 already names the **session**. Do not invent a “best set” rule. Safe default **supported by the decision:** expose that session’s **completed** sets for the exercise (decision 6). |
| In-progress collision UX (resume vs discard messaging) | `mvp-decisions.md` Q2, session lifecycle | NON-BLOCKING | Client flow only | Product already forbids a second in-progress session. HLD/API: reject a second start; expose get-in-progress + discard. No further product rule required. |
| Incomplete set rows kept vs dropped on complete | `mvp-decisions.md` Q3; decision 6 | NON-BLOCKING | Complete use-case internals; history payload shape | Decision 6 only requires they **not count** as completed data. Same logical set entity with a completion flag is enough. Do **not** invent a requirement to delete rows. History/previous-performance queries must ignore incomplete sets. |
| Live timer vs duration from timestamps | `mvp-decisions.md` Q4; `feature.md` §9; decision 25 | NON-BLOCKING | Active-session display | Completed duration = `completedAt - startedAt`. Do not store a competing product duration. In-progress display is not a second duration definition. |
| Default planned set count if the user enters none | `mvp-decisions.md` Q5; decision 8 | NON-BLOCKING | Routine create/edit validation | Documents require a planned set count; they do **not** specify N. Do not invent “default 3”. Treat count as **required input** on each routine exercise. |
| Same exercise twice on one **session** | `mvp-decisions.md` Q6; decision 21 (routines only) | NON-BLOCKING | Unique constraint scope | Do **not** add a session-level uniqueness rule. Only routine-level uniqueness is specified. |
| Un-archive custom exercise | `mvp-decisions.md` Q7; decision 17 | NON-BLOCKING | Optional future API | MVP requires hide/archive from future selection. Do not invent restore. Archive can be modeled so restore could be added later; no restore API in MVP HLD. |
| Dashboard “this week” and timezone | `feature.md` §§5, 16; `mvp-decisions.md` Q8; streak deferred | NON-BLOCKING | Dashboard stats **read** API only; not the logging schema | No week rule is approved. Do **not** invent ISO week vs rolling 7 days. Persist completion timestamps; define the week query when product specifies it. Totals of completed workouts/sets do not depend on week. |
| Dedicated Progress screen / historical table without charts or PRs | `feature.md` §§14, 18–19, Flow 3; `mvp-decisions.md` P0 progress slice and Q9 | NON-BLOCKING | Whether HLD includes a Progress route and list API | Decisions require **previous performance on the active session** (P0) and grouping by identity when available. They do **not** confirm or remove a Progress destination. HLD must include the previous-performance read. Do **not** invent a Progress screen, and do **not** treat `feature.md` §18 as unmodified. If a Progress UI is added later, it can read the same completed-set history. |
| §14 “Best Weight / Best Reps” aggregation | `feature.md` §14; `product-review.md`; charts/PRs deferred | NON-BLOCKING | Only if a Progress table is built | No approved formula. Do not design a “best” aggregation API until product defines it or confirms the Progress table. |
| Desktop standalone Exercises item | `feature.md` §§18–19; `mvp-decisions.md` Q10 vs “unlisted sections remain in force”; decision 28 (mobile) | NON-BLOCKING | Desktop IA | Decision 28 constrains **mobile**. `mvp-decisions.md` also says unlisted `feature.md` sections remain unless they depend on a deferred feature. Desktop Exercises is therefore **not overridden**. HLD may keep a desktop catalog entry point; mobile uses Add Exercise. Q10 does not create a second rule. |
| Exercise/workout notes | `feature.md` §9, §13 vs §22 P2; `mvp-decisions.md` P2 not re-opened | DEFERRED | Notes fields and APIs | P2. Omit from MVP HLD/API. |
| Training volume, PR types, charts, rest timer, streak, duplicate routine | `feature.md` §§10, 14–16, 22; `mvp-decisions.md` P1/P2 | DEFERRED | Extra modules and tables | Out of MVP HLD except as named P1/P2 follow-ups. |
| Supersets / RPE listed as out of scope and as P2 | `feature.md` §21 vs §22 | DEFERRED | None for MVP | Do not design. |
| Weight/reps decimals, zero, maxima | `mvp-decisions.md` Q12; `feature.md` examples in kg | NON-BLOCKING | Validation only | No approved limits. Do not invent plate increments. kg-only is decided. Negative values are not described as valid logging. |
| Destructive-action confirmations | `mvp-decisions.md` Q13 | NON-BLOCKING | UX | APIs still discard/delete/archive as specified. |
| Muscle-group filter: primary vs secondary | `feature.md` §7; `mvp-decisions.md` Q14 | NON-BLOCKING | List query | Filter is specified; match rule is not. Do not invent “primary only.” Exercise still has a primary muscle group for display. |
| Greeting copy | `feature.md` §5; decision 24 | NON-BLOCKING | UI | Display name exists and is required. How it is interpolated is unspecified. |
| Catalog size and exact seed list | `mvp-decisions.md` Q16; decision 20 | NON-BLOCKING | Seed content | Product provides a curated catalog. Exact rows are content, not HLD blockers. |
| Password policy; email uniqueness as an explicit sentence | `mvp-decisions.md` Q17; `feature.md` §4 | NON-BLOCKING | Auth validation | Email+password login is specified. Uniqueness of email as the login identifier is a necessary invariant of that mechanism, not a new user-facing feature. Password complexity is unspecified; do not invent a policy in the spec. |
| Reorder exercises during an in-progress session | `product-review.md`; `feature.md` order on routine and session exercise | NON-BLOCKING | Optional session command | Routine reorder is specified. Live reorder is not. Session exercises still have order when started from a routine. Do not invent a reorder-during-session requirement. |
| Add exercise mid-session after starting from a routine | `feature.md` §8 empty workout vs routine start | NON-BLOCKING | Session update | Empty sessions can add exercises. Removing from an in-progress session is allowed. Adding after a routine start is not forbidden. Do not invent a restriction. |
| Snapshot wording “when the session was created” vs exercises added later | `mvp-decisions.md` decision 30 vs empty-session add | NON-BLOCKING | When name is captured on a session exercise | Completed history must keep the name that belonged to that logged exercise. Empty sessions add exercises after start. HLD should snapshot name on the session exercise when it is placed on the session so later catalog edits cannot rewrite it. That implements decision 30/17 without adding a new product feature. |
| Auth session lifetime / “remember me” | `product-review.md` | NON-BLOCKING | Security mechanism | Persistence of **workout** data across sessions is specified. Auth token lifetime is implementation, not an open product feature for logging. |
| Tablet layout | `.cursor/rules/03-frontend.mdc` vs `feature.md` §17 | NON-BLOCKING | Responsive breakpoints | Desktop + mobile are specified. Treat tablet as a layout of those, not a new product mode. |
| Pagination of history | `product-review.md` | NON-BLOCKING | List API | History is reverse chronological. Page size is not a product rule. |
| Custom vs catalog name collisions | `product-review.md` | NON-BLOCKING | Unique name constraints | Not specified. Do not invent global unique names. |

---

## 7. Final Recommendation

The approved decisions close the issues that would have forced architects to guess session state, history integrity, MVP cut line, units, and ownership. Remaining items are presentation, deferred P1/P2, or optional read-model details. Conceptual architecture, logical data model, and MVP API resource design can proceed **without** treating leftover `feature.md` narrative as current scope.

READY FOR HLD
