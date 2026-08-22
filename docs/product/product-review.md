# Workout Tracker — MVP Product Requirements Review

**Status:** Review of `docs/product/feature.md` against project constraints in `.cursor/rules/`.  
**Scope:** Product and domain analysis only. This document does not change `feature.md` and does not specify an implementation.  
**Audience:** Product and engineering, before detailed design and coding.

Where architecture options appear, they are **trade-offs to decide**, not decisions already made.

---

## Summary

The feature spec describes a coherent strength-training loop: create reusable routines, log sets during a session, then review history and basic progress. That loop matches the product vision (fast logging, not coaching) and the project context (routines, logging, history, basic progress, historical accuracy when templates change).

The spec is **not yet implementation-ready**.

- **Priority, screens, success criteria, and the data model disagree.** Several behaviors written as MVP features are P1, optional, or have no fields in §20.
- **In-progress sessions, Settings, auth recovery, units, and catalog seeding are unspecified.**
- **History integrity is required by project rules** (“completed workout data must remain historically accurate”) but the proposed model stores live `exerciseId` links with **no snapshots** of name or classification.

**Recommended MVP bar (product, not architecture):** a new user can sign up, build Push / Pull / Legs routines from a seeded library plus custom exercises, start / log / complete a session (from a routine or empty), and later view that session accurately even if routines or exercises change. Success criterion 8 requires **previous-session comparison**; charts, personal records, streak, rest timer, and duplicate-routine can remain P1 unless explicitly promoted.

Until the open questions in this review are answered, implementation will be forced to invent product behavior.

---

## MVP Assessment

### What is solid

- Clear vision: logging speed over coaching complexity.
- Sensible user segments: beginners, routine-based gym-goers, progressive-overload trackers.
- Core loop is understandable: routine → start → log sets → complete → history.
- Email/password auth, private per-user data, and persistence across sessions are stated.
- Routine create / edit / delete, exercise library browse / search / filter, custom exercises, set logging, complete, history, and details are described.
- Mobile-first active workout and desktop/mobile layout principles match project frontend rules.
- Out-of-scope list (social, AI, wearables, coaching, payments) is appropriately large.

### What is misaligned with a simple MVP

| Area | Issue |
| --- | --- |
| Success vs priority | §25 requires comparing current vs previous performance. §22 lists previous performance as **P1**. |
| Feature vs priority | Rest timer, charts, PRs, streak, duplicate routine are fully specified in feature sections but **P1**. |
| Nav vs requirements | Desktop nav includes **Settings** (§19) with **no** Settings feature. |
| Notes | §9 lists exercise notes as an active-workout action; §22 puts exercise notes (and workout notes) in **P2**. §13 shows notes on details. |
| Volume | §11 optional volume formula; §22 P2; §15 PR type “highest training volume in a single set” needs volume. |
| Planned sets | §8 loads planned sets from a routine; `RoutineExercise` has only `order` (§20). |
| History vs model | Project rules require snapshots in spirit; `WorkoutExercise` has `exerciseId` only. |
| Out of scope vs P2 | §21 excludes supersets and RPE; §22 P2 lists them again. |
| Screens vs P0 | §18 includes Progress; §22 puts charts/PRs as P1; P0 omits Progress as a must-have screen. |
| Streak | Dashboard §5 includes streak; §22 lists streak as P1. §16 contradicts itself (see Ambiguities). |

### Fit against project constraints

- **Do not add features outside approved MVP** — the spec currently mixes P0, P1, P2, and out-of-scope items on the same screens (Active Workout, Dashboard, Progress, Complete).
- **Preserve historical data when routines change** — not reflected in §20. Completing a session that only stores FKs will show **today’s** exercise name/muscles when the user views last month’s workout after a rename or catalog edit.
- **Mobile usability of active workout** — well stated; rest timer auto-start, notes, and PRs on complete add taps and chrome that compete with “minimal clicks.”

### Verdict

The **P0 slice** (auth, routines, library, log, complete, history, details, responsive UI) is a viable first product **if** in-progress lifecycle, snapshots, empty-workout start, and completion rules are added. Charts, PRs, streak, rest timer, Settings, and duplicate routine should not block calling the first slice done unless product explicitly pulls them into P0.

---

## Ambiguities

This section includes **unclear wording** and **direct contradictions** between sections of `feature.md` (and vs `.cursor/rules/`).

### Terminology: workout vs routine vs session

Copy uses “workout” for templates, live sessions, and history records. Navigation is “Workouts” (desktop and mobile). Entities are `WorkoutRoutine` and `WorkoutSession`. Dashboard actions mix “Start a new workout” and “Start an existing workout routine.” Implementers cannot tell whether “Workouts” is the routine list, the history list, or both.

### Priority vs narrative

| Topic | Written as | Also written as |
| --- | --- | --- |
| Rest timer | §10 MVP requirements; §18 Active Workout purpose | §22 **P1** |
| Previous performance | §9 Active Workout; §25 success criterion 8 | §22 **P1** |
| Streak / basic stats | §5 Dashboard; §16 | §22 **P1** (streak, basic statistics) |
| Duplicate routine | §6 requirements | §22 **P1** |
| Exercise notes | §9 user actions | §22 **P2** |
| Workout notes | §13 details display | §22 **P2**; no session field in §20 |
| Training volume | §11 optional on complete | §22 **P2**; needed for volume PR in §15 |
| Supersets / RPE | §21 **out of scope** | §22 **P2** |

### Streak

§16 first allows “consecutive calendar days **or** planned workout days,” then says MVP is “consecutive days **ending today** where the user completed at least one workout.” Those are different products: rest days reset the streak vs planned-split streaks. “Ending today” means a rest day shows **0** even after a long run.

### Muscle groups and filters

- Heading “Optional secondary muscle group” (singular) vs example “Triceps, Shoulders” vs model `secondaryMuscleGroups`.
- Filter “by muscle group” does not say primary only, secondary, or either.
- Legs is a single group; no glutes, hamstrings, or calves. Cardio and bodyweight are not muscle groups or categories.

### Exercise category

Example category is “Barbell.” There is no allowed list (barbell, dumbbell, machine, cable, bodyweight, other). MVP does not filter by category; P2 mentions “advanced filters.”

### Planned sets

§8: starting from a routine loads “planned sets.” The model has no set count, target weight, or target reps. Unspecified: default empty rows (e.g. 3), last-session copy, or user-defined targets on the routine.

### Previous performance

Unspecified which session (last completed containing that exercise vs last session overall), which set when there are many, and whether incomplete or skipped sets count. Example is a single “50 kg × 10.”

### Progress table “Best Weight” / “Best Reps”

Unspecified whether maxima are per calendar day, per session, or per set, and whether best weight and best reps may come from **different** sets (e.g. 60 kg × 3 and 40 kg × 15 shown on one row).

### Personal records

- Highest weight: across all sets ever, or in a valid completed set only?
- Highest repetitions: unbounded by weight rewards empty-bar high-rep sets.
- Highest volume in a **single set** needs `weight × reps` even if session volume is P2.
- Celebration copy is specified; placement (complete summary vs toast vs Progress) is not.
- Bodyweight / 0 kg is undefined.

### Rest timer

“User can configure the duration” does not say global default, per exercise, per routine, or per set. Auto-start is “optional.” Persistence across refresh, sound/vibration, and behavior when the screen locks are unspecified.

### Dashboard Start Workout

Primary button vs “start existing routine” vs empty workout (§8) is not a single flow. First-time users with zero routines are unspecified.

### Skip vs remove

§9 allows skip **or** remove an exercise from the current workout. Skip might keep a row marked skipped; remove might delete it. History and volume implications differ.

### Completing a session

Unspecified whether the user may complete with incomplete sets, zero completed sets, or skipped exercises. Unspecified whether incomplete set rows are stored.

### Units

Examples use kg only. No lbs, no user preference, no storage unit.

### “This week” and time

Workouts this week: ISO week (Mon–Sun), locale week, or rolling 7 days? Timezone: user profile, browser, or UTC? Affects streak and dashboard stats.

### Greeting and `User.name`

Dashboard greeting is unspecified (first name, email local-part, time of day). `name` is on the user entity; signup does not say it is required.

### Duration

Live timer, `completedAt - startedAt`, and stored `duration` can diverge (tab backgrounded, device clock change, user leaving the screen open overnight). Source of truth is unspecified.

### Navigation gaps

Desktop: Dashboard, Workouts, Exercises, History, Progress, Settings.  
Mobile primary: Home, Workouts, History, Progress.  
No mobile path to **Exercises** or **Settings**. Frontend rules also require **tablet**; §17 only names desktop and mobile.

### Auth vs product model

§20 includes `passwordHash`. That is an implementation detail, not a product field, and must not appear in APIs. Email uniqueness, verification, password policy, and reset are not specified.

### Delete vs history

Delete routine, delete history record, and (unspecified) delete custom exercise vs `workoutRoutineId` / `exerciseId` on past sessions is unspecified. Project rules forbid mutating completed records when **routines** change; they do not define FK delete behavior.

---

## Missing Requirements

Includes missing **product rules** and missing **user flows**. §23 only documents three happy paths.

### Account and access

- Email uniqueness.
- Password policy.
- Email verification (yes/no for MVP).
- Password reset / forgot password.
- Stay logged in / session lifetime (product expectation, not mechanism).
- Account deletion and what happens to data.
- Logout confirmation (yes/no).

### Settings and preferences

- Settings screen contents, or explicit removal from nav.
- Units (kg / lbs).
- Default rest duration storage.
- Timezone (or “always use device local time” as a product rule).

### In-progress session lifecycle

- Whether at most one session may be in progress per user.
- Save and **resume** after refresh, close, or another device.
- **Discard / cancel** vs complete.
- Starting a second workout while one is open.
- Client-only draft until complete vs server-persisted in-progress session (see trade-offs under Edge Cases / data model).

### Logging rules

- Validation: decimal weights (e.g. 2.5), zero weight, negative values, max weight/reps, empty fields when marking complete.
- Whether the same exercise may appear twice in one routine or session.
- Reorder during an **active** session (routine reorder is specified; live reorder is not).
- Add exercise mid-session (library + custom) is implied by empty workout; not spelled out for routine-based sessions.
- Default weight/reps (blank vs copy previous set vs copy last session).

### Exercise catalog

- Seed list size, content, and ownership (product-defined catalog vs empty until custom).
- Custom exercise **edit** and **delete**.
- Name collisions (custom vs catalog, two customs).
- Creating a custom exercise from the library vs only while building a routine / live session.

### Routines and completion

- Planned set count and/or targets, or explicit “no targets in MVP.”
- Duplicate routine: naming (`Copy of Push Day`?) and what is copied.
- Completing with incomplete/zero sets.
- Whether completed sessions may be **edited** or only viewed/deleted.
- Confirmation on delete routine / delete history.

### History integrity (product rules, not schema)

- What must remain stable on a completed session when the user later edits a routine, renames a custom exercise, or deletes a catalog link.
- Display name if the linked exercise is gone.

### Progress and stats

- Chart date range (all time vs last N weeks).
- Empty states (no sessions, no data for an exercise).
- Week and streak definitions (see Open Questions).
- Pagination or infinite scroll for history.

### Quality bar (product-facing)

- Empty states for dashboard, routines, library, history, progress.
- Error states (save failed, session expired).
- Destructive-action confirmations.
- Offline gym use (explicit in or out).
- Accessibility and one-handed logging beyond “large buttons.”
- Legal: privacy policy / terms (if accounts exist).

### Missing user flows

Not in §23, but required to implement or to explicitly defer:

1. Sign up, log in, log out.
2. Forgot password / reset (if in MVP).
3. First-run dashboard with **zero** routines.
4. Start **empty** workout, name it, add exercises, complete.
5. Resume in-progress workout after interruption.
6. Discard / abandon in-progress workout.
7. Start workout while another is already in progress.
8. Add / skip / remove / reorder exercises during a live session.
9. Create / edit / delete custom exercise.
10. Duplicate routine (if P0/P1 kept).
11. Delete a history record; delete or edit a routine after sessions exist.
12. Browse exercise library on **mobile**.
13. Open Settings / account (if kept).
14. View progress / PRs with no history.
15. Complete workout with skipped or incomplete sets.
16. Search/filter with no matches.

---

## Edge Cases

### Session and time

- User leaves a session open overnight; duration and “today” for streak/week stats.
- Two completed sessions on the same calendar day (streak still 1 day; “workouts this week” is 2).
- Device timezone change mid-week.
- Browser tab backgrounded: timer freeze vs wall-clock elapsed.
- Two tabs: two in-progress editors, last write wins or conflict.
- User completes with all sets incomplete, or completes after skipping every exercise.

### Catalog and templates

- Rename or delete a custom exercise used in past sessions and in current routines.
- Delete a routine that in-progress or completed sessions point at via `workoutRoutineId`.
- Edit a routine after many sessions; history must not change (project rule).
- Same exercise twice in one session (two Bench Press blocks): previous performance and PRs per block vs merged.
- Empty search/filter; exercise name longer than the UI allows.

### Logging values

- Weight `0` (bodyweight) and volume/PRs.
- Very large weight/reps (typos).
- Decimal plates (2.5 kg) vs integers only.
- Mark set complete with blank weight or reps.

### Rest timer

- Screen lock / app background during countdown.
- Complete a set, start rest, then remove that set.
- Last set of the last exercise: timer still useful?

### Progress and PRs

- First session ever: no previous performance.
- PR on first logged set (everything is a PR).
- Delete a history record that held the only PR.
- Best weight and best reps on different sets the same day.

### Data-model implications (options, not decisions)

Project database rules: completed data stays accurate; routine changes must not rewrite history; prefer relational models and constraints.

**1. Exercise identity on a completed session**

- **Snapshot name (and optionally muscle/category) plus optional `exerciseId`.** History display is stable; progress can still group by id when the exercise exists. Extra writes and a rule for grouping if the id is later deleted.
- **Live `exerciseId` only (as in §20).** Simpler schema; **violates** historical accuracy if names or muscles change.
- **Snapshot only, no FK.** Maximum isolation; harder to build “progress for this exercise” when names diverge.

Recommendation for product: require **stable display of what was logged**. Engineering then chooses snapshot+FK vs snapshot-only.

**2. Session lifecycle**

- **Same `WorkoutSession` row with status** `IN_PROGRESS` | `COMPLETED` | `ABANDONED`, unique one in-progress per user. Resume and multi-device are straightforward; must define discard vs complete.
- **Insert session only on complete; active workout is client state.** Fewer server rows and no abandon records; refresh and second device lose the log unless local storage is specified.
- **Local draft synced periodically.** Better gym resilience; conflict and auth-expiry behavior must be specified.

**3. `workoutRoutineId` on sessions**

- **Nullable** for empty workouts (required if §8 empty start stays).
- **ON DELETE SET NULL:** user can delete routines; history keeps the session **name** if snapshotted. Routine link is lost.
- **ON DELETE RESTRICT:** history stays linked; users cannot delete routines that were used. Frustrating if they want to clean templates.

**4. Planned sets**

- Add `plannedSetCount` (and optionally target weight/reps) on `RoutineExercise`.
- Or drop “planned sets” from MVP and always start with a fixed empty row count (product rule, e.g. one empty set).

**5. Notes**

- `WorkoutExercise.notes` exists in §20. Session-level notes are missing if §13 “Notes” means workout notes. Either add a field or drop workout notes from details until P2.

**6. Sets**

- Nullable weight/reps until `isCompleted`; decide whether incomplete rows persist after complete.
- Constraints (`weight >= 0`, `repetitions > 0` when completed) belong in product rules then database checks.

**7. Personal records**

- **Compute on read** from completed sets: no extra tables; Progress and “new PR” can drift if definitions change; heavier queries.
- **Persist on complete:** fast Progress UI and an audit of which session created the PR; definitions become migration-sensitive; deletes must recompute.

**8. Units**

- **kg-only MVP:** no preference field; fastest.
- **Store as entered + unit:** simple display, painful comparison if the user switches unit.
- **Store canonical kg, display by preference:** consistent progress/PRs; rounding when switching to lbs.

**9. Rest timer**

- **Client-only** with a user default (90s): no backend; lost on full reload unless local storage is a product requirement.
- **Server default on user profile:** consistent across devices; still a client countdown.

**10. Secondary muscles**

- Array/column: simple, weaker filtering.
- Join table: filter/query friendly, more schema.

**11. Product vs security fields**

- Do not treat `passwordHash` as a product or API attribute. Auth storage is an implementation concern under Spring Security.

**12. Duration**

- Store `startedAt` / `completedAt` in UTC and **derive** duration for display, **or** store elapsed seconds captured by the client. Mixing both without a rule creates support bugs.

---

## Recommended Changes

These are **product spec** recommendations for a later edit of `feature.md`. They are not implemented here.

### 1. Glossary (use everywhere)

- **Exercise** — catalog or custom movement.
- **Routine** — reusable template (name, description, ordered exercises, optional planned sets).
- **Session** — a logged workout (in progress or completed), with snapshotted display data.

Replace overloaded “workout” in nav and buttons (e.g. Routines vs History vs Start session).

### 2. Align P0 with success criteria and history rules

**P0 (must ship for “MVP successful”):** authentication (signup/login/logout), dashboard (without unresolved Settings), routine CRUD, seeded library + custom exercises, start from routine **and** empty session, log weight/reps/sets, add/remove sets, duration, complete, history, details, **historical snapshots**, **in-progress resume/discard**, responsive desktop/mobile, **previous performance** *or* remove success criterion 8.

**Keep as P1 unless promoted:** rest timer, charts, PRs, streak, duplicate routine, extra dashboard stats beyond “last session + start CTA.”

### 3. Remove or demote from MVP surface

Until explicitly in P0, **do not** present as required on MVP screens:

- Settings in navigation (or ship a minimal account menu: logout only).
- Auto-start rest timer.
- Session total volume on the summary **if** volume remains P2 (or promote volume if volume PRs stay).
- Exercise notes and workout notes (P2).
- Supersets and RPE (already out of scope; remove from P2).
- Dark mode, data export, progress photos, advanced filters (already P2; keep them out of screen lists).
- Duplicate routine as a §6 “must” while it is P1.

### 4. Add to P0 (currently missing)

- Discard and resume in-progress session; at most one in-progress session (or an explicit multi-session rule).
- Snapshot of exercise **name** (minimum) on session exercises.
- Empty-session start (already in §8; missing from P0 list and primary flows).
- Custom exercise edit/delete and collision rules.
- Delete/FK product rules for routines and exercises vs history.
- kg-only **or** a unit preference (pick one).
- Completion rules (incomplete sets allowed or not).
- Seed catalog expectation (even “small curated list of N compounds”).

### 5. Rest timer

Pick one:

- **P1:** remove from Active Workout “purpose” and from Flow 2 until later.
- **P0-lite:** global default 90s, user-adjustable, **manual** start, client-side countdown only. No auto-start.

### 6. Align screens and nav

- Mobile: path to exercise library (e.g. from add-exercise, or a fifth dest, or under Routines).
- Settings: specify fields or remove from §18–§19.
- Tablet: one sentence that tablet follows desktop nav or compact mobile nav.

### 7. Define formulas and rules in the spec

- Week boundary and timezone.
- Streak: consecutive days ending today (rest day = 0) **or** another definition — not both.
- Planned sets: count only vs targets vs none.
- Previous performance: e.g. last **completed** session that includes this exercise, last **completed** set (or best set — pick one).
- PR formulas, including 0 kg and deleted sessions.
- Progress row: one session per row vs one calendar day; same-set vs independent maxima.

### 8. Data model in the spec (conceptual)

Update §20 in a future spec pass to include session status, nullable routine id, snapshots, and planned-set fields **after** product picks the options above. Do not keep `passwordHash` in the product model.

### 9. Documentation set

Project rules point at `docs/architecture/`, `docs/database/`, `docs/api/`, `docs/features/`, `docs/diagrams/`. Those folders are not populated yet. After product answers the open questions, those docs should be written so implementation is not inferred from this review alone.

---

## Open Questions

Answers are required before implementation. Suggested defaults are **not** decisions.

1. **Previous performance:** Is it P0 (success criterion 8) or P1? If P1, will success criteria be revised?
2. **In-progress sessions:** At most one per user? Resume after refresh, close, and other devices?
3. **Complete rules:** May the user complete with incomplete sets, zero completed sets, or skipped exercises? Are incomplete rows stored?
4. **Units:** kg only for MVP, or kg and lbs?
5. **Planned sets:** None (N empty rows), user-defined count on the routine, and/or target weight/reps?
6. **Streak:** Consecutive calendar days ending today (rest day resets to 0), or last-N-days-with-a-session, or planned-split days?
7. **“This week”:** ISO week vs last 7 days? Timezone: profile vs device vs UTC?
8. **Settings:** In MVP? If yes, which items (units, rest default, password change, logout only)?
9. **Password reset and email verification:** In MVP or deferred? If deferred, what happens if the user forgets the password?
10. **Completed sessions:** View and delete only, or edit after complete?
11. **Custom exercises:** Edit and delete in MVP? If delete, what happens to routines and history (snapshot vs block delete vs hide from library)?
12. **PR definitions:** Max weight, max reps (at any weight?), max single-set volume; bodyweight / 0 kg; first-session PRs; recompute after history delete?
13. **Previous performance selection:** Which session and which set? Include skipped/incomplete?
14. **Rest timer:** Global vs per exercise? Auto-start yes/no? In P0 or P1?
15. **Skip vs remove:** Are these different? What is stored on the session?
16. **Seed catalog:** Approximate size and who owns the list? Can users see only their customs plus global catalog?
17. **Duplicate exercise:** Allowed twice in one routine or one session?
18. **Offline:** Must logging work without network in the gym, or is online-only acceptable for MVP?
19. **Dashboard primary CTA:** Empty session, routine picker, or last routine?
20. **User display name:** Required at signup, optional, or derived from email?
21. **Duration source of truth:** Wall clock between start/complete timestamps vs client-measured elapsed time?
22. **Same exercise progress:** Group by `exerciseId` after rename, or by snapshotted name?
23. **Delete routine:** Allowed if sessions reference it? What remains on the session card?
24. **Mobile Exercises / Settings:** How does the user reach them without desktop nav?
25. **Account deletion:** In MVP? Hard delete vs retain anonymized history (legal vs product)?
