package com.workouttracker.workout.history.api;

import java.time.Instant;
import java.util.UUID;

public record HistorySummaryResponse(
        UUID id,
        String name,
        Instant completedAt,
        Long durationSeconds,
        int exerciseCount,
        int completedSetCount
) {
}
