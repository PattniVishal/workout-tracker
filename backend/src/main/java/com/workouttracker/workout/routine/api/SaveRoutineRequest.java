package com.workouttracker.workout.routine.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Create or update workout routine request")
public record SaveRoutineRequest(
        @Schema(description = "Routine name", maxLength = 200) @NotBlank @Size(max = 200) String name,
        @Schema(description = "Optional description", maxLength = 2000, nullable = true) @Size(max = 2000) String description,
        @Schema(description = "Ordered routine exercises") @NotNull List<@Valid RoutineExerciseRequest> exercises
) {
    @Schema(description = "Exercise slot within a routine")
    public record RoutineExerciseRequest(
            @Schema(description = "Exercise ID") @NotNull UUID exerciseId,
            @Schema(description = "Planned number of sets", minimum = "1") @NotNull @Min(1) Integer plannedSetCount
    ) {
    }
}
