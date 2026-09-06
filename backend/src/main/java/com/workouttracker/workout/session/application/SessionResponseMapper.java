package com.workouttracker.workout.session.application;

import com.workouttracker.workout.session.api.SessionExerciseResponse;
import com.workouttracker.workout.session.api.SessionResponse;
import com.workouttracker.workout.session.api.SessionSetResponse;
import com.workouttracker.workout.session.domain.WorkoutExercise;
import com.workouttracker.workout.session.domain.WorkoutSession;
import com.workouttracker.workout.session.domain.WorkoutSet;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class SessionResponseMapper {

    public SessionResponse toResponse(WorkoutSession session) {
        List<SessionExerciseResponse> exercises = session.getExercises().stream()
                .sorted(Comparator.comparingInt(WorkoutExercise::getPosition))
                .map(this::toExerciseResponse)
                .toList();

        return new SessionResponse(
                session.getId(),
                session.getName(),
                session.getStatus().name(),
                session.getOriginRoutineId(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.durationSeconds(),
                exercises
        );
    }

    private SessionExerciseResponse toExerciseResponse(WorkoutExercise exercise) {
        List<SessionSetResponse> sets = exercise.getSets().stream()
                .sorted(Comparator.comparingInt(WorkoutSet::getSetNumber))
                .map(this::toSetResponse)
                .toList();

        return new SessionExerciseResponse(
                exercise.getId(),
                exercise.getExerciseId(),
                exercise.getExerciseName(),
                exercise.getPosition(),
                sets
        );
    }

    private SessionSetResponse toSetResponse(WorkoutSet set) {
        return new SessionSetResponse(
                set.getId(),
                set.getSetNumber(),
                set.getWeightKg(),
                set.getRepetitions(),
                set.isCompleted()
        );
    }
}
