package com.workouttracker.workout.history.application;

import com.workouttracker.common.security.CurrentUser;
import com.workouttracker.workout.history.api.HistoryListResponse;
import com.workouttracker.workout.history.api.HistorySummaryResponse;
import com.workouttracker.workout.history.api.PreviousPerformanceResponse;
import com.workouttracker.workout.history.api.PreviousPerformanceSetResponse;
import com.workouttracker.workout.session.api.SessionResponse;
import com.workouttracker.workout.session.application.SessionResponseMapper;
import com.workouttracker.workout.session.domain.WorkoutExercise;
import com.workouttracker.workout.session.domain.WorkoutSession;
import com.workouttracker.workout.session.domain.WorkoutSessionStatus;
import com.workouttracker.workout.session.domain.WorkoutSet;
import com.workouttracker.workout.session.infrastructure.WorkoutSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkoutHistoryService {

    private static final Logger log = LoggerFactory.getLogger(WorkoutHistoryService.class);

    private final WorkoutSessionRepository workoutSessionRepository;
    private final CurrentUser currentUser;
    private final SessionResponseMapper sessionResponseMapper;

    public WorkoutHistoryService(
            WorkoutSessionRepository workoutSessionRepository,
            CurrentUser currentUser,
            SessionResponseMapper sessionResponseMapper
    ) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.currentUser = currentUser;
        this.sessionResponseMapper = sessionResponseMapper;
    }

    @Transactional(readOnly = true)
    public HistoryListResponse listCompleted() {
        UUID userId = currentUser.requireUserId();
        List<HistorySummaryResponse> workouts = workoutSessionRepository
                .findCompletedDetailedByUserIdOrderByCompletedAtDesc(userId, WorkoutSessionStatus.COMPLETED)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
        return new HistoryListResponse(workouts);
    }

    @Transactional(readOnly = true)
    public SessionResponse getCompleted(UUID sessionId) {
        UUID userId = currentUser.requireUserId();
        WorkoutSession session = requireCompletedSession(sessionId, userId);
        return sessionResponseMapper.toResponse(session);
    }

    @Transactional
    public void deleteCompleted(UUID sessionId) {
        UUID userId = currentUser.requireUserId();
        WorkoutSession session = requireCompletedSession(sessionId, userId);
        workoutSessionRepository.delete(session);
        workoutSessionRepository.flush();
        log.info("Workout history deleted userId={} sessionId={}", userId, sessionId);
    }

    @Transactional(readOnly = true)
    public Optional<PreviousPerformanceResponse> getPreviousPerformance(UUID exerciseId) {
        UUID userId = currentUser.requireUserId();
        List<WorkoutSession> sessions = workoutSessionRepository
                .findCompletedSessionsContainingExerciseOrderByCompletedAtDesc(
                        userId,
                        WorkoutSessionStatus.COMPLETED,
                        exerciseId,
                        PageRequest.of(0, 1)
                );

        if (sessions.isEmpty()) {
            return Optional.empty();
        }

        WorkoutSession session = sessions.getFirst();
        WorkoutExercise exercise = session.getExercises().stream()
                .filter(workoutExercise -> workoutExercise.getExerciseId().equals(exerciseId))
                .min(Comparator.comparingInt(WorkoutExercise::getPosition))
                .orElseThrow();

        List<PreviousPerformanceSetResponse> sets = exercise.getSets().stream()
                .filter(WorkoutSet::isCompleted)
                .sorted(Comparator.comparingInt(WorkoutSet::getSetNumber))
                .map(set -> new PreviousPerformanceSetResponse(
                        set.getSetNumber(),
                        set.getWeightKg(),
                        set.getRepetitions()
                ))
                .toList();

        if (sets.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new PreviousPerformanceResponse(
                exercise.getExerciseId(),
                exercise.getExerciseName(),
                session.getCompletedAt(),
                session.getId(),
                sets
        ));
    }

    private WorkoutSession requireCompletedSession(UUID sessionId, UUID userId) {
        return workoutSessionRepository
                .findDetailedByIdAndUserIdAndStatus(sessionId, userId, WorkoutSessionStatus.COMPLETED)
                .orElseThrow(HistorySessionNotFoundException::new);
    }

    private HistorySummaryResponse toSummaryResponse(WorkoutSession session) {
        int completedSetCount = (int) session.getExercises().stream()
                .flatMap(exercise -> exercise.getSets().stream())
                .filter(WorkoutSet::isCompleted)
                .count();

        return new HistorySummaryResponse(
                session.getId(),
                session.getName(),
                session.getCompletedAt(),
                session.durationSeconds(),
                session.getExercises().size(),
                completedSetCount
        );
    }
}
