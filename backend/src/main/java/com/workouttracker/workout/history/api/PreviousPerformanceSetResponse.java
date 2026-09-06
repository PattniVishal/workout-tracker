package com.workouttracker.workout.history.api;

import java.math.BigDecimal;

public record PreviousPerformanceSetResponse(
        int setNumber,
        BigDecimal weightKg,
        Integer repetitions
) {
}
