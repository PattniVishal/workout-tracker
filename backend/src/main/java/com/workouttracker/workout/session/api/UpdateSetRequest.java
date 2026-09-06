package com.workouttracker.workout.session.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

@Schema(description = "Update workout set request")
public record UpdateSetRequest(
        @Schema(description = "Weight in kilograms", minimum = "0", nullable = true) @DecimalMin(value = "0") BigDecimal weightKg,
        @Schema(description = "Repetitions", minimum = "0", nullable = true) @Min(0) Integer repetitions,
        @Schema(description = "Whether the set is completed", nullable = true) Boolean completed
) {
}
