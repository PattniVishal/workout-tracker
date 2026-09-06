package com.workouttracker.exercise.domain;

public enum ExerciseSource {
    SYSTEM,
    CUSTOM;

    public static ExerciseSource fromCreatedByUserId(java.util.UUID createdByUserId) {
        return createdByUserId == null ? SYSTEM : CUSTOM;
    }
}
