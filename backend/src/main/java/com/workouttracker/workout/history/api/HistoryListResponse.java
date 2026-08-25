package com.workouttracker.workout.history.api;

import java.util.List;

public record HistoryListResponse(List<HistorySummaryResponse> workouts) {
}
