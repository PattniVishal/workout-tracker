package com.workouttracker.workout.session.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Add exercise to the current workout session")
public record AddSessionExerciseRequest(
        @Schema(description = "Exercise ID to add") @NotNull UUID exerciseId,
        @Schema(description = "Initial number of sets to create", minimum = "1") @NotNull @Min(1) Integer initialSetCount
) {
}
