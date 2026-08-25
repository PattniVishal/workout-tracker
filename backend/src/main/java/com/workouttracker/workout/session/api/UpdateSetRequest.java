package com.workouttracker.workout.session.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdateSetRequest(
        @DecimalMin(value = "0") BigDecimal weightKg,
        @Min(0) Integer repetitions,
        Boolean completed
) {
}
