package com.workouttracker.workout.routine.api;

import java.util.UUID;

public record RoutineExerciseResponse(
        UUID id,
        UUID exerciseId,
        String exerciseName,
        int plannedSetCount,
        int position
) {
}
