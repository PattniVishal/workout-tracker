package com.workouttracker.exercise.api;

import java.util.List;
import java.util.UUID;

public record ExerciseResponse(
        UUID id,
        String name,
        String primaryMuscleGroup,
        List<String> secondaryMuscleGroups,
        String category,
        String source
) {
}
