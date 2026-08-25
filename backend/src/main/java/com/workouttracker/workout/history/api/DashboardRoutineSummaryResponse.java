package com.workouttracker.workout.history.api;

import java.util.UUID;

public record DashboardRoutineSummaryResponse(
        UUID id,
        String name,
        int exerciseCount
) {
}
