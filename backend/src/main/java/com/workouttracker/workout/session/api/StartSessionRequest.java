package com.workouttracker.workout.session.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Start workout session request. Provide exactly one of routineId or name.")
public record StartSessionRequest(
        @Schema(description = "Routine to start from", nullable = true) UUID routineId,
        @Schema(description = "Ad-hoc workout name when not starting from a routine", nullable = true) String name
) {
}
