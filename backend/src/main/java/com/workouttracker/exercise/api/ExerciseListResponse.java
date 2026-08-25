package com.workouttracker.exercise.api;

import java.util.List;

public record ExerciseListResponse(List<ExerciseResponse> exercises) {
}
