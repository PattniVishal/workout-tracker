package com.workouttracker.workout.session.api;

import java.util.UUID;

public record StartSessionRequest(
        UUID routineId,
        String name
) {
}
