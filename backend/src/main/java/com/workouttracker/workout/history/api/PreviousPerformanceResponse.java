package com.workouttracker.workout.history.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PreviousPerformanceResponse(
        UUID exerciseId,
        String exerciseName,
        Instant completedAt,
        UUID sessionId,
        List<PreviousPerformanceSetResponse> sets
) {
}
