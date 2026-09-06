package com.workouttracker.workout.session.api;

import java.util.List;
import java.util.UUID;

public record SessionExerciseResponse(
        UUID id,
        UUID exerciseId,
        String exerciseName,
        int position,
        List<SessionSetResponse> sets
) {
}
