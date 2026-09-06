package com.workouttracker.workout.routine.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoutineSummaryResponse(
        UUID id,
        String name,
        String description,
        int exerciseCount,
        Instant updatedAt
) {
}
