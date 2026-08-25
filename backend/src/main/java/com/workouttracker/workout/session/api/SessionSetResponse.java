package com.workouttracker.workout.session.api;

import java.math.BigDecimal;
import java.util.UUID;

public record SessionSetResponse(
        UUID id,
        int setNumber,
        BigDecimal weightKg,
        Integer repetitions,
        boolean completed
) {
}
