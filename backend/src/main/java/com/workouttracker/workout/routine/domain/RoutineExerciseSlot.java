package com.workouttracker.workout.routine.domain;

import java.util.UUID;

public record RoutineExerciseSlot(
        UUID exerciseId,
        int position,
        int plannedSetCount
) {
}
