package com.workouttracker.workout.session.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String name,
        String status,
        UUID originRoutineId,
        Instant startedAt,
        Instant completedAt,
        Long durationSeconds,
        List<SessionExerciseResponse> exercises
) {
}
