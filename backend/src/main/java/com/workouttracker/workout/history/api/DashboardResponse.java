package com.workouttracker.workout.history.api;

import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        String displayName,
        boolean hasActiveSession,
        UUID activeSessionId,
        List<DashboardRoutineSummaryResponse> routines,
        DashboardMostRecentCompletedResponse mostRecentCompleted,
        long totalCompletedWorkouts,
        long totalCompletedSets
) {
}
