package com.workouttracker.exercise.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Create or update custom exercise request")
public record CreateExerciseRequest(
        @Schema(description = "Exercise name", maxLength = 200) @NotBlank @Size(max = 200) String name,
        @Schema(description = "Primary muscle group", maxLength = 50) @NotBlank @Size(max = 50) String primaryMuscleGroup,
        @Schema(description = "Secondary muscle groups") @NotNull List<@NotBlank @Size(max = 50) String> secondaryMuscleGroups,
        @Schema(description = "Exercise category", maxLength = 50) @NotBlank @Size(max = 50) String category
) {
}
