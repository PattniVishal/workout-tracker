package com.workouttracker.workout.history.api;

import java.time.Instant;
import java.util.UUID;

public record DashboardMostRecentCompletedResponse(
        UUID id,
        String name,
        Instant completedAt
) {
}
