# Workout Tracker App – Feature Requirements

## 1. Product Overview

### Product Name

Workout Tracker

### Product Vision

Build a simple and easy-to-use workout tracking application that helps users:

* Create workout routines
* Add exercises to workouts
* Log sets, reps, and weight during a workout
* Track their workout history
* Monitor progress over time

The primary goal is to make workout logging **fast and simple**, rather than building a complex fitness coaching platform.

---

# 2. Target Users

The application is designed for:

### Beginner Gym Users

Users who want a simple way to record what exercises they perform and how much weight they lift.

### Regular Gym-Goers

Users who follow predefined workout routines such as:

* Push / Pull / Legs
* Upper / Lower
* Full Body
* Custom workout splits

### Strength Training Users

Users who want to track progressive overload by comparing their current performance with previous workouts.

---

# 3. MVP Scope

The first version should focus on the following core user journey:

> Create Workout → Add Exercises → Start Workout → Log Sets → Complete Workout → View History and Progress

The application should avoid unnecessary complexity in the initial version.

---

# 4. Core Features

## Feature 1: User Authentication

### Description

Users should be able to create an account and securely access their workout data.

### Requirements

* User can sign up
* User can log in
* User can log out
* User data should be private to the authenticated user
* Workout data should persist across sessions

### Authentication Methods

For MVP:

* Email and password authentication

Optional future enhancement:

* Google Sign-In
* Apple Sign-In

---

# 5. Dashboard / Home Screen

## Description

The dashboard should provide a quick overview of the user's workout activity.

### Requirements

Display:

* Greeting
* Primary **Start Workout** button
* List of saved workout routines
* Most recent workout
* Basic workout statistics

### Example Statistics

* Workouts completed this week
* Total workouts completed
* Current workout streak

### Primary Actions

Users should be able to:

* Start a new workout
* Start an existing workout routine
* Create a new workout routine
* View workout history

---

# 6. Workout Routine Management

## Description

Users should be able to create reusable workout routines.

Examples:

* Push Day
* Pull Day
* Leg Day
* Chest & Triceps
* Back & Biceps
* Full Body

### Create Workout Routine

A user can create a workout with:

* Workout name
* Optional description
* List of exercises

### Example

**Workout Name:** Push Day

Exercises:

1. Bench Press
2. Incline Dumbbell Press
3. Overhead Press
4. Lateral Raises
5. Tricep Pushdown

### Requirements

Users should be able to:

* Create a workout routine
* Edit a workout routine
* Delete a workout routine
* Duplicate a workout routine
* Reorder exercises within a workout

---

# 7. Exercise Library

## Description

The application should provide a predefined library of common exercises.

### Each Exercise Should Contain

* Exercise name
* Primary muscle group
* Optional secondary muscle group
* Exercise category

### Example Exercise

**Name:** Bench Press
**Primary Muscle:** Chest
**Secondary Muscles:** Triceps, Shoulders
**Category:** Barbell

### Initial Muscle Groups

* Chest
* Back
* Shoulders
* Biceps
* Triceps
* Legs
* Core

### MVP Requirements

Users should be able to:

* Browse exercises
* Search exercises by name
* Filter exercises by muscle group
* Add an exercise to a workout

### Custom Exercises

Users should also be able to create a custom exercise.

Example:

**Exercise Name:** Smith Machine Incline Press

Custom exercises should belong only to the user who created them.

---

# 8. Start Workout

## Description

Users should be able to start a workout from:

1. An existing workout routine
2. A new empty workout

### When Starting From a Routine

The application should automatically load:

* Workout name
* Exercises
* Planned sets

### Empty Workout

The user can:

* Enter a workout name
* Add exercises while working out
* Log sets

---

# 9. Active Workout Screen

## Description

This is the most important screen in the application.

The user should be able to log their workout quickly with minimal clicks.

### Display

The screen should show:

* Workout name
* Workout start time
* Workout duration timer
* List of exercises

For each exercise, display:

* Exercise name
* Set number
* Weight
* Repetitions
* Set completion status

### Example

**Bench Press**

| Set | Weight | Reps | Status    |
| --- | -----: | ---: | --------- |
| 1   |  40 kg |   12 | Completed |
| 2   |  50 kg |   10 | Completed |
| 3   |  50 kg |    8 | Completed |

### User Actions

For each exercise, users can:

* Add a set
* Remove a set
* Enter weight
* Enter repetitions
* Mark a set as completed
* Add exercise notes
* Skip or remove an exercise from the current workout

### Previous Performance

When possible, show the user's previous workout performance for the same exercise.

Example:

> Previous workout: 50 kg × 10 reps

This helps users track progressive overload.

---

# 10. Rest Timer

## Description

After completing a set, the user should be able to start a rest timer.

### MVP Requirements

* Default rest duration: 90 seconds
* User can configure the duration
* Timer counts down
* User can stop or restart the timer

Optional:

* Automatically start the timer when a set is marked as completed

---

# 11. Complete Workout

## Description

When the user finishes exercising, they should be able to complete the workout.

### On Completion

Store:

* Workout name
* Start time
* End time
* Total duration
* Exercises performed
* Sets completed
* Weight and repetitions for each set

### Workout Summary

Show a summary containing:

* Workout name
* Total duration
* Number of exercises
* Total sets completed

Optional calculation:

* Total training volume

Formula:

> Total Volume = Sum of (Weight × Repetitions)

---

# 12. Workout History

## Description

Users should be able to view all previously completed workouts.

### Requirements

Display workouts in reverse chronological order.

Each workout card should show:

* Workout name
* Completion date
* Workout duration
* Number of exercises
* Number of completed sets

### User Actions

Users should be able to:

* View workout details
* Delete a workout record

---

# 13. Workout Details

## Description

Users should be able to view the complete details of a completed workout.

### Display

* Workout name
* Date
* Start and end time
* Total duration
* Exercises
* Sets
* Weight
* Repetitions
* Notes

Example:

**Bench Press**

* Set 1: 40 kg × 12
* Set 2: 50 kg × 10
* Set 3: 50 kg × 8

---

# 14. Progress Tracking

## Description

Users should be able to track basic workout progress.

The initial version should remain simple.

### Exercise Progress

For each exercise, show historical performance.

Example:

**Bench Press**

| Date   | Best Weight | Best Reps |
| ------ | ----------: | --------: |
| Aug 01 |       50 kg |         8 |
| Aug 08 |       50 kg |        10 |
| Aug 15 |       55 kg |         8 |

### Charts

Provide a simple progress chart for:

* Maximum weight lifted over time
* Training volume over time

Users should be able to select an exercise and view its progress.

---

# 15. Personal Records

## Description

The application should identify simple personal records.

### Initial PR Types

* Highest weight lifted
* Highest repetitions
* Highest training volume in a single set

Example:

> 🎉 New Personal Record!
> Bench Press: 60 kg

This feature should be calculated automatically when a workout is completed.

---

# 16. Basic Statistics

## Dashboard Statistics

Display:

* Total workouts completed
* Workouts completed this week
* Total sets completed
* Current workout streak

### Definitions

**Workout Streak**

The number of consecutive calendar days or planned workout days on which the user completed a workout.

For MVP, use the simpler definition:

> Number of consecutive days ending today where the user completed at least one workout.

---

# 17. Responsive Design

The application must work well on both:

* Desktop / Monitor
* Mobile phones

### Desktop Experience

Use:

* Sidebar navigation
* Wider workout tables
* Dashboard cards
* Charts

### Mobile Experience

Use:

* Bottom navigation or compact navigation
* Large touch-friendly buttons
* Stacked exercise cards
* Easy number input for weight and reps

### Important Principle

The active workout logging experience should be optimized for mobile because users may use the application while exercising.

---

# 18. Core Screens

The MVP should contain the following screens.

## 1. Login / Sign Up

Purpose:

* Authenticate users

---

## 2. Dashboard

Purpose:

* Start workouts
* View routines
* View basic statistics

---

## 3. Workout Routines

Purpose:

* View all saved routines
* Create a new routine

---

## 4. Create / Edit Workout Routine

Purpose:

* Enter workout name
* Add exercises
* Reorder exercises
* Save the routine

---

## 5. Exercise Library

Purpose:

* Browse exercises
* Search exercises
* Filter by muscle group
* Create custom exercises

---

## 6. Active Workout

Purpose:

* Log sets
* Enter weight and repetitions
* Track workout duration
* Use rest timer
* Complete workout

---

## 7. Workout History

Purpose:

* View completed workouts

---

## 8. Workout Details

Purpose:

* View detailed information about a completed workout

---

## 9. Progress

Purpose:

* Select exercises
* View historical performance
* View simple charts
* View personal records

---

# 19. Navigation

## Desktop Navigation

* Dashboard
* Workouts
* Exercises
* History
* Progress
* Settings

## Mobile Navigation

Primary navigation:

* Home
* Workouts
* History
* Progress

The **Start Workout** action should always be easily accessible.

---

# 20. MVP Data Model

The initial application will likely require the following core entities.

## User

```text
User
- id
- name
- email
- passwordHash
- createdAt
```

## Exercise

```text
Exercise
- id
- name
- primaryMuscleGroup
- secondaryMuscleGroups
- category
- isCustom
- createdByUserId
```

## Workout Routine

```text
WorkoutRoutine
- id
- userId
- name
- description
- createdAt
- updatedAt
```

## Routine Exercise

```text
RoutineExercise
- id
- workoutRoutineId
- exerciseId
- order
```

## Completed Workout

```text
WorkoutSession
- id
- userId
- workoutRoutineId
- name
- startedAt
- completedAt
- duration
```

## Workout Exercise

```text
WorkoutExercise
- id
- workoutSessionId
- exerciseId
- order
- notes
```

## Workout Set

```text
WorkoutSet
- id
- workoutExerciseId
- setNumber
- weight
- repetitions
- isCompleted
```

---

# 21. Out of Scope for MVP

The following features should NOT be built initially.

They can be considered in future versions.

## Social Features

* Follow other users
* Social feed
* Share workouts
* Comments
* Likes

## AI Features

* AI-generated workouts
* AI fitness coach
* AI exercise recommendations

## Advanced Workout Features

* Supersets
* Drop sets
* Failure sets
* Warm-up calculators
* RPE tracking
* Advanced periodization

## Integrations

* Smartwatches
* Apple Health
* Google Fit
* Wearables

## Advanced Analytics

* Muscle recovery calculations
* Advanced body-part volume analysis
* Training balance analysis
* Advanced 1RM calculations

## Coaching

* Trainer accounts
* Coach-client relationships
* Assigned workout plans
* Chat with coaches

## Payments

* Subscriptions
* Premium plans
* Payment processing

---

# 22. MVP Prioritization

## P0 — Must Have

The application is not considered usable without these features.

* User authentication
* Dashboard
* Create workout routine
* Edit workout routine
* Delete workout routine
* Exercise library
* Custom exercises
* Start workout
* Log weight and repetitions
* Add and remove sets
* Workout duration tracking
* Complete workout
* Workout history
* Workout details
* Responsive desktop and mobile design

---

## P1 — Should Have

These features significantly improve the user experience.

* Rest timer
* Previous workout performance
* Basic exercise progress charts
* Personal records
* Workout streak
* Basic statistics
* Duplicate workout routine

---

## P2 — Nice to Have

These can be added after the MVP is stable.

* Exercise notes
* Workout notes
* Training volume calculations
* Advanced filters
* Data export
* Dark mode
* Supersets
* RPE tracking
* Progress photos

---

# 23. Primary User Flows

## Flow 1: Create a Workout Routine

```text
Dashboard
    ↓
Create Workout
    ↓
Enter Workout Name
    ↓
Add Exercises
    ↓
Arrange Exercise Order
    ↓
Save Workout
```

---

## Flow 2: Perform a Workout

```text
Dashboard
    ↓
Select Workout Routine
    ↓
Start Workout
    ↓
Perform Exercise
    ↓
Enter Weight + Repetitions
    ↓
Complete Set
    ↓
Rest Timer
    ↓
Repeat for Remaining Exercises
    ↓
Complete Workout
    ↓
View Workout Summary
```

---

## Flow 3: Track Progress

```text
Progress
    ↓
Select Exercise
    ↓
View Previous Performances
    ↓
View Weight Progress Chart
    ↓
View Personal Record
```

---

# 24. Product Principles

The application should follow these principles:

## Fast

Logging a set should require minimal interaction.

## Simple

Avoid overwhelming users with too many metrics.

## Useful

Every major screen should help the user either:

* Plan a workout
* Log a workout
* Understand their progress

## Mobile Friendly

The application should be comfortable to use in a gym with one hand.

## Responsive

The same application should provide a good experience on both mobile and desktop.

---

# 25. MVP Success Criteria

The MVP can be considered successful if a new user can:

1. Sign up
2. Create a Push / Pull / Legs workout routine
3. Add exercises to each routine
4. Start a workout from their routine
5. Log weight and repetitions for every set
6. Complete the workout
7. View the completed workout later
8. Compare their current exercise performance with previous workouts

The entire primary workflow should be understandable without requiring a tutorial.
