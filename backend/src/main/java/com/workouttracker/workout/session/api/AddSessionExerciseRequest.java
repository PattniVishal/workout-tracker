package com.workouttracker.workout.session.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddSessionExerciseRequest(
        @NotNull UUID exerciseId,
        @NotNull @Min(1) Integer initialSetCount
) {
}
