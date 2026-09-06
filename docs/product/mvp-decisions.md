# Workout Tracker — Approved MVP Product Decisions

**Status:** Approved product clarification for MVP planning.  
**Sources:** `docs/product/feature.md`, `docs/product/product-review.md`, `.cursor/rules/`.  
**Precedence:** Where this document conflicts with `feature.md`, **`mvp-decisions.md` is the newer approved clarification.** `feature.md` is not edited here; it should be aligned in a later spec pass.

This document records **only** the approved decisions listed below. It does not add architecture, database, or implementation design. It does not invent further product rules.

---

## Decision Summary

These decisions resolve the blocking gaps identified in the product review: what is P0 vs P1, how a live session behaves, how history stays accurate, and what the first release will not include.

| # | Decision |
| --- | --- |
| 1 | Previous performance is **P0**. |
| 2 | A user can have **at most one** in-progress workout session. |
| 3 | In-progress sessions are **persisted** and can be **resumed** after browser refresh, close, or login from another device. |
| 4 | Users can **discard** an in-progress session. |
| 5 | A workout can be **completed only when at least one set** has been marked completed. |
| 6 | **Incomplete set rows** are not considered completed workout data. |
| 7 | MVP supports **kg only**. |
| 8 | Routines support **planned set count only**. No target weight or target repetitions. |
| 9 | **Streak** is deferred to **P1**. |
| 10 | **Personal records** are deferred to **P1**. |
| 11 | **Progress charts** are deferred to **P1**. |
| 12 | **Rest timer** is deferred to **P1**. |
| 13 | **Duplicate routine** is deferred to **P1**. |
| 14 | There is **no dedicated Settings screen** in the MVP. |
| 15 | **Password reset** and **email verification** are deferred from the MVP. |
| 16 | Completed sessions can be **viewed and deleted**, not edited. |
| 17 | Custom exercises can be **created and edited**. Deletion is **hide/archive** from future selection, not a break of historical data. |
| 18 | Previous performance is the **latest completed session** that contains the same exercise. |
| 19 | **Skip and remove are not separate.** Users can **remove** an exercise from an in-progress workout. |
| 20 | There is a **curated global exercise catalog**, plus **user custom exercises**. |
| 21 | The same exercise **cannot appear more than once** in the same **routine**. |
| 22 | The MVP is **online-only**. Offline-first synchronization is out of scope. |
| 23 | Dashboard primary **Start Workout** lets the user **choose an existing routine or start an empty workout**. |
| 24 | **User display name is required** during signup. |
| 25 | Workout **duration is calculated from `startedAt` and `completedAt`**. |
| 26 | Progress for an exercise is grouped by **stable exercise identity when available**. |
| 27 | Users **can delete routines** even when completed sessions originated from them. Completed sessions stay accurate via **snapshots**. |
| 28 | On **mobile**, exercises are reached through **Add Exercise** during routine creation or workout logging. |
| 29 | **Account deletion** is out of scope for the MVP. |
| 30 | Historical completed workout data must preserve **at minimum the exercise name** as it existed when the session was created. |

---

## Conflicts with `feature.md`

`mvp-decisions.md` overrides `feature.md` in these places. Unlisted sections of `feature.md` remain in force unless they depend on a deferred feature.

| Topic | What `feature.md` says | Approved clarification |
| --- | --- | --- |
| Previous performance priority | §22 lists it as **P1**. §9 and §25 treat it as part of the core experience. | **P0** (decision 1). |
| Rest timer | §10 titled “MVP Requirements”; §18 Active Workout purpose includes rest timer; Flow 2 includes Rest Timer. | **Deferred to P1** (decision 12). Not required on the Active Workout screen or in the primary perform-workout flow for MVP. |
| Streak | §5 and §16 show current workout streak on the dashboard; §16 defines an MVP streak rule. §22 already lists streak as P1. | **Deferred to P1** (decision 9). Dashboard MVP must not require streak. |
| Personal records | §15 is a full feature; §18 Progress purpose includes PRs; Flow 3 includes View Personal Record. | **Deferred to P1** (decision 10). |
| Progress charts | §14 Charts; §17 desktop “Charts”; §18 Progress “View simple charts”; Flow 3 includes the weight progress chart. | **Deferred to P1** (decision 11). |
| Duplicate routine | §6 lists duplicate as a user requirement. §22 lists it as P1. | **Deferred to P1** (decision 13). Not required in routine management for MVP. |
| Settings | §19 desktop navigation includes Settings. | **No dedicated Settings screen** in MVP (decision 14). Settings must not appear as a required destination. |
| Skip vs remove | §9: “Skip or remove an exercise from the current workout.” | One action: **remove** (decision 19). |
| Planned sets | §8 loads “planned sets” with no definition of targets vs count. §20 has no planned-set fields. | **Planned set count only**; no target weight or reps (decision 8). |
| Duration | §11 stores total duration; §9 shows a live duration timer; §20 lists `duration` alongside start/end times. | Duration for a **completed** session is **calculated from `startedAt` and `completedAt`** (decision 25). |
| Auth recovery | §4 requires signup, login, logout; it does not specify reset or verification. | **Password reset and email verification are deferred** (decision 15). |
| Exercise library on mobile | §18 includes a dedicated Exercise Library screen; §19 mobile primary nav omits Exercises. | On mobile, catalog access is through **Add Exercise** in routine creation or logging (decision 28). |
| History vs live catalog | §20 session exercises are tied to `exerciseId` only; no snapshot rule. Project rules already require history not to change when routines change. | Completed data must keep **at least the exercise name** from session creation (decision 30). Routine delete must not rewrite that history (decision 27). |
| Units | Examples use kg; lbs is never specified. | **kg only** (decision 7). Explicit: no unit switching in MVP. |
| Display name | User has `name`; signup does not say it is required. | **Required at signup** (decision 24). |

Features already listed as P1 in §22 (rest timer, charts, PRs, streak, duplicate) that also appear as full feature sections or screen purposes are **in** `feature.md` as narrative and **out** of MVP delivery until P1, per this document.

---

## MVP Scope

The MVP is successful when a signed-in user can plan routines, log a session, and later see that session and prior performance for the same exercise—without streak, PRs, charts, rest timer, duplicate routine, Settings, password reset, email verification, account deletion, or offline sync.

### In scope

- **Account:** Sign up (email, password, **required display name**), log in, log out. Data is private to the authenticated user.
- **Dashboard:** Greeting, primary **Start Workout** (choose a routine **or** start empty), list of saved routines, most recent workout, basic statistics **excluding streak**.
- **Routines:** Create, edit, delete, reorder exercises. Optional description. **Planned set count** per routine exercise. **No** duplicate-routine action. **No** duplicate exercise in the same routine. Deleting a routine is allowed even if sessions were started from it.
- **Exercises:** Curated **global catalog**. Users can **create and edit** custom exercises (custom exercises belong to that user). Browse, search, and filter as described in `feature.md`, with mobile access via **Add Exercise**. Hide/archive custom exercises from future selection without breaking history.
- **Session start:** From a routine (load name, exercises, planned set counts) or empty (user names the session and adds exercises while logging).
- **Active session:** Log weight (kg) and repetitions; add/remove sets; mark sets completed; **remove** an exercise from the session; show **previous performance** (latest completed session that contains that exercise); duration display consistent with start time. **No** rest timer.
- **Complete:** Allowed only if **at least one set** is marked completed. Summary as in `feature.md` except features deferred here (no PRs, no rest timer, no required volume/streak).
- **History:** Reverse-chronological completed sessions; open details; **delete** a completed record; **no edit**.
- **Progress (P0 slice):** Users can compare current logging with **previous performance** while working out (success criterion 8). Grouping of an exercise’s progress uses **stable exercise identity when available**. Charts and personal records are **not** in this slice.
- **Responsive UI:** Desktop and mobile. Active logging is mobile-first. Online-only.

### Explicitly not in this MVP slice

See [Deferred Features](#deferred-features) and out-of-scope items already in `feature.md` §21 (social, AI, wearables, coaching, payments, advanced set types, and so on). Account deletion is also out of scope (decision 29).

---

## Deferred Features

Deferred to **P1** (or later, as noted). Do not build them as part of the MVP bar.

| Feature | Timing | Notes |
| --- | --- | --- |
| Previous performance | **Not deferred** | P0 (decision 1). Listed here only because `feature.md` §22 called it P1. |
| Rest timer (including auto-start) | P1 | Remove from MVP Active Workout and Flow 2. |
| Progress charts | P1 | Remove from MVP Progress purpose and Flow 3. |
| Personal records | P1 | Remove from MVP Progress purpose, complete celebration, and Flow 3. |
| Workout streak | P1 | Remove from MVP dashboard statistics. |
| Duplicate workout routine | P1 | Remove from MVP routine requirements. |
| Settings screen | Not in MVP | No dedicated screen. Logout remains available through authentication (existing §4). |
| Password reset | Deferred from MVP | Users cannot recover a forgotten password in this slice. |
| Email verification | Deferred from MVP | Signup does not require a verified inbox. |
| Account deletion | Out of scope for MVP | Decision 29. |
| Offline-first sync | Out of scope | Online-only (decision 22). |
| Target weight / target reps on routines | Not in MVP | Planned **count** only (decision 8). |
| Skip as distinct from remove | Not in MVP | Remove only (decision 19). |
| lbs / unit preference | Not in MVP | kg only (decision 7). |

`feature.md` §22 **P2** items (exercise notes, workout notes, training volume, export, dark mode, supersets, RPE, progress photos, advanced filters) were **not** re-opened by the approved decision list. They remain as in `feature.md` (P2 or out of scope). This document does not promote or cancel them.

---

## Explicit Product Rules

Rules below are restatements of approved decisions. They are the product contract for MVP planning.

1. **Weight unit:** All logged and displayed weights are kilograms. No unit picker.
2. **Display name:** Signup cannot complete without a display name.
3. **Catalog:** The product provides a curated global exercise list. Users may add custom exercises that only they can use for selection (in addition to the global catalog).
4. **Custom exercise lifecycle:** Create and edit are allowed. “Delete” means the exercise is hidden/archived so it is not offered for **future** routines or sessions. Historical completed data is not broken by that action.
5. **Routine uniqueness:** An exercise may be added at most once to a given routine.
6. **Planned sets:** When a session starts from a routine, each exercise is prepared using that exercise’s **planned set count** only—not suggested weight or reps.
7. **Start Workout:** The primary dashboard action offers **choose an existing routine** or **start empty**.
8. **One live session:** A user cannot have two in-progress sessions at the same time.
9. **Persistence:** An in-progress session survives refresh, browser close, and login on another device, and can be resumed.
10. **Discard:** The user may throw away an in-progress session (it is not a completed history record).
11. **Complete gate:** Complete is disabled or rejected unless at least one set is marked completed.
12. **Completed set data:** Only sets marked completed count as completed workout data. Incomplete rows do not.
13. **Remove exercise:** From an in-progress session, the user may remove an exercise. There is no separate skip.
14. **Previous performance:** For an exercise in the active session, show performance from the **most recently completed** session that included that exercise (when one exists).
15. **Completed sessions:** View and delete only; no edit after complete.
16. **Routine delete vs history:** Routines may be deleted even if past sessions came from them. Those sessions remain historically accurate (snapshots).
17. **Progress identity:** When grouping progress for an exercise, use a **stable exercise identity when it is available**.
18. **History name:** Completed sessions must keep **at least the exercise name** as it was when the session was created.
19. **Duration:** For a completed session, duration is **`completedAt` minus `startedAt`** (the product definition of duration).
20. **Mobile catalog:** Users reach exercises on mobile by adding an exercise while building a routine or logging a session—not via a required standalone mobile Exercises tab.
21. **No Settings screen.**
22. **Online-only:** Features assume a network connection. Offline-first synchronization is out of scope.
23. **No password reset, email verification, or account deletion** in this MVP.

---

## Session Lifecycle Rules

Applies to a **session** (a logged workout), not to a **routine** (a template).

```text
Start (routine or empty)
    → In progress (persisted; at most one per user)
        → Resume (refresh, close, or another device)
        → Discard (not history)
        → Complete (only if ≥ 1 set marked completed)
            → History (view / delete; no edit)
```

- **Start from routine:** Session is named from the routine; exercises and **planned set counts** are loaded. The session is not a live copy of the routine: later routine edits do not rewrite this session once it exists as logged data (see Historical Data Rules).
- **Start empty:** User supplies a name and adds exercises during the session.
- **In progress:** There is at most one. It is stored so the same user can continue after refresh, close, or another device. Logging remains **online-only**.
- **If the user already has an in-progress session:** They cannot start a second one (decision 2). How the UI points them at resume vs discard is not further specified (see Remaining Non-Blocking Questions).
- **Discard:** Ends the in-progress session without creating a completed history record.
- **Complete:** Requires at least one set marked completed. Incomplete set rows are not completed workout data. After complete, the session appears in history and may be viewed or deleted, not edited.
- **Duration:** When the session is completed, duration is calculated from `startedAt` and `completedAt`.

---

## Historical Data Rules

These rules satisfy the project constraint that completed workout data stay accurate when routines (or catalog entries) change later.

1. **Minimum snapshot:** For each exercise on a **completed** session, preserve **at least the exercise name** as it existed when the session was created (decision 30). Later catalog or custom-exercise renames must not change that historical name.
2. **Routine changes:** Editing a routine after a session was logged must not change that session’s stored exercises, sets, weights, or reps.
3. **Routine deletion:** Allowed even when completed sessions originated from that routine. Those sessions remain viewable and historically accurate. They are not deleted as a side effect of deleting the routine.
4. **Custom exercise hide/archive:** Removing a custom exercise from future selection must not break or rewrite completed session display (name snapshot at minimum).
5. **Completed session deletion:** The user may delete a history record. That is an explicit user action, not a side effect of routine or exercise changes.
6. **Progress grouping:** When building progress for an exercise, group by **stable exercise identity when available** (decision 26), so rename/archive does not silently split or lose the user’s history of that movement when identity still exists.
7. **Incomplete sets:** Incomplete set rows are not part of completed workout data (decision 6). History and previous performance must not treat them as completed sets.

This document does not specify storage shape, foreign keys, or APIs.

---

## Terminology Glossary

Use these terms in product and engineering conversation. UI copy may still say “workout” where it is clearer for users, but specs and tickets should not mix the three concepts.

| Term | Meaning |
| --- | --- |
| **Exercise** | A movement in the global catalog or a user’s custom list. |
| **Routine** | A reusable template: name, optional description, ordered exercises, planned set count per exercise. |
| **Session** | A logged workout instance: empty or started from a routine; in progress, discarded, or completed. |
| **In-progress session** | The at-most-one active session the user can resume or discard. |
| **Completed session** | A session that passed the complete gate and appears in history. |
| **Planned set count** | How many set rows a routine exercise should create when a session starts. Not a target weight or rep number. |
| **Previous performance** | Logging from the latest **completed** session that contains the same exercise. |
| **Global catalog** | Product-curated exercises available to all users. |
| **Custom exercise** | User-created exercise, owned by that user. |
| **Hide / archive** | Custom exercise is not offered for new selection; historical sessions keep their snapshotted name. |
| **Remove (session)** | Take an exercise off the in-progress session. Not a distinct “skip.” |

“Workouts” in `feature.md` navigation may mean routines, sessions, or both; implementation planning should map that label to **Routines** vs **History** explicitly rather than treating it as a third entity.

---

## Remaining Non-Blocking Questions

The following were **not** answered by the approved decision list. They must not block MVP planning against the rules above. Resolve them in UX copy, a later spec pass, or implementation defaults **without contradicting** this document.

1. **Previous performance presentation:** Decision 18 names the source session, not which set(s) to show when that session has multiple sets (last set, all sets, or another summary).
2. **In-progress collision UX:** How to tell the user they already have a session (resume prompt, discard prompt, or both) when they hit Start Workout.
3. **Incomplete rows at complete time:** Whether incomplete set rows are dropped from the completed record or kept visibly as incomplete but excluded from completed-set counts and previous performance.
4. **Elapsed time while in progress:** Decision 25 defines duration for a **completed** session. The live timer on the Active Workout screen is still described in `feature.md` §9; how it relates to `startedAt` before `completedAt` exists is unspecified.
5. **Default planned set count** when adding an exercise to a routine if the user does not enter a count.
6. **Same exercise twice in one session** (empty or after removes/adds). Decision 21 applies to **routines** only.
7. **Un-archive:** Whether a hidden custom exercise can be restored to the picker.
8. **Dashboard “this week”** and timezone for “workouts completed this week” / “total workouts” (streak is deferred; other stats in `feature.md` §5 / §16 were not redefined except excluding streak).
9. **Progress screen without charts or PRs:** Whether MVP still includes a Progress destination with a historical table, or only previous performance on the active session.
10. **Desktop Exercise Library:** Decision 28 is for **mobile**. Whether desktop keeps a standalone Exercises item (`feature.md` §19) is unset.
11. **Exercise notes / workout notes:** Still P2 in `feature.md`; not mentioned in the approved list. Do not treat them as newly in or out beyond `feature.md`.
12. **Weight/reps validation:** Decimals (e.g. 2.5 kg), zero weight, and max values were not decided.
13. **Confirmations** on discard, delete routine, delete history, or archive custom exercise.
14. **Muscle-group filter:** Primary only vs primary or secondary (`feature.md` ambiguity).
15. **Greeting copy** (how display name is used).
16. **Catalog size and exact contents** of the curated global list.
17. **Password policy and email uniqueness** (expected for any email/password product, not specified here).

If a later decision is needed on these items, record it explicitly rather than inferring it from `feature.md` alone.
