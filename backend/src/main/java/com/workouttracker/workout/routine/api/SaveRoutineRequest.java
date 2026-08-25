package com.workouttracker.workout.routine.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SaveRoutineRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotNull List<@Valid RoutineExerciseRequest> exercises
) {
    public record RoutineExerciseRequest(
            @NotNull UUID exerciseId,
            @NotNull @Min(1) Integer plannedSetCount
    ) {
    }
}
