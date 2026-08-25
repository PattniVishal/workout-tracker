package com.workouttracker.workout.routine.api;

import java.util.List;
import java.util.UUID;

public record RoutineDetailResponse(
        UUID id,
        String name,
        String description,
        List<RoutineExerciseResponse> exercises
) {
}
