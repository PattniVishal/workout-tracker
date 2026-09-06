package com.workouttracker.workout.session.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Workout session response")
public record SessionResponse(
        @Schema(description = "Session ID") UUID id,
        @Schema(description = "Workout name") String name,
        @Schema(description = "Session status", example = "IN_PROGRESS") String status,
        @Schema(description = "Origin routine ID when started from a routine", nullable = true) UUID originRoutineId,
        @Schema(description = "Session start timestamp") Instant startedAt,
        @Schema(description = "Completion timestamp", nullable = true) Instant completedAt,
        @Schema(description = "Duration in seconds when completed", nullable = true) Long durationSeconds,
        @Schema(description = "Exercises in the session") List<SessionExerciseResponse> exercises
) {
}
