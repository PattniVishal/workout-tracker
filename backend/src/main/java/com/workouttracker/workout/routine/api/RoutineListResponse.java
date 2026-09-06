package com.workouttracker.workout.routine.api;

import java.util.List;

public record RoutineListResponse(List<RoutineSummaryResponse> routines) {
}
