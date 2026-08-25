package com.workouttracker.exercise.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateExerciseRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 50) String primaryMuscleGroup,
        @NotNull List<@NotBlank @Size(max = 50) String> secondaryMuscleGroups,
        @NotBlank @Size(max = 50) String category
) {
}
