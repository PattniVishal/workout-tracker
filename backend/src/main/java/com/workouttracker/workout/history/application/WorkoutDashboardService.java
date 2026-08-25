package com.workouttracker.workout.history.application;

import com.workouttracker.common.security.AuthenticationRequiredException;
import com.workouttracker.common.security.CurrentUser;
import com.workouttracker.common.security.UserPrincipal;
import com.workouttracker.workout.history.api.DashboardMostRecentCompletedResponse;
import com.workouttracker.workout.history.api.DashboardResponse;
import com.workouttracker.workout.history.api.DashboardRoutineSummaryResponse;
import com.workouttracker.workout.routine.domain.WorkoutRoutine;
import com.workouttracker.workout.routine.infrastructure.WorkoutRoutineRepository;
import com.workouttracker.workout.session.domain.WorkoutSession;
import com.workouttracker.workout.session.domain.WorkoutSessionStatus;
import com.workouttracker.workout.session.infrastructure.WorkoutSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkoutDashboardService {

    private final CurrentUser currentUser;
    private final WorkoutRoutineRepository workoutRoutineRepository;
    private final WorkoutSessionRepository workoutSessionRepository;

    public WorkoutDashboardService(
            CurrentUser currentUser,
            WorkoutRoutineRepository workoutRoutineRepository,
            WorkoutSessionRepository workoutSessionRepository
    ) {
        this.currentUser = currentUser;
        this.workoutRoutineRepository = workoutRoutineRepository;
        this.workoutSessionRepository = workoutSessionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        UserPrincipal principal = currentUser.getPrincipal()
                .orElseThrow(AuthenticationRequiredException::new);
        UUID userId = principal.getId();

        boolean hasActiveSession = workoutSessionRepository.existsByUserIdAndStatus(
                userId,
                WorkoutSessionStatus.IN_PROGRESS
        );
        UUID activeSessionId = hasActiveSession
                ? workoutSessionRepository.findSessionIdByUserIdAndStatus(userId, WorkoutSessionStatus.IN_PROGRESS)
                .orElse(null)
                : null;

        List<DashboardRoutineSummaryResponse> routines = workoutRoutineRepository
                .findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toRoutineSummary)
                .toList();

        DashboardMostRecentCompletedResponse mostRecentCompleted = workoutSessionRepository
                .findFirstByUserIdAndStatusOrderByCompletedAtDesc(userId, WorkoutSessionStatus.COMPLETED)
                .map(this::toMostRecentCompleted)
                .orElse(null);

        long totalCompletedWorkouts = workoutSessionRepository.countByUserIdAndStatus(
                userId,
                WorkoutSessionStatus.COMPLETED
        );
        long totalCompletedSets = workoutSessionRepository.countCompletedSetsByUserIdAndStatus(
                userId,
                WorkoutSessionStatus.COMPLETED
        );

        return new DashboardResponse(
                principal.getDisplayName(),
                hasActiveSession,
                activeSessionId,
                routines,
                mostRecentCompleted,
                totalCompletedWorkouts,
                totalCompletedSets
        );
    }

    private DashboardRoutineSummaryResponse toRoutineSummary(WorkoutRoutine routine) {
        return new DashboardRoutineSummaryResponse(
                routine.getId(),
                routine.getName(),
                routine.getExercises().size()
        );
    }

    private DashboardMostRecentCompletedResponse toMostRecentCompleted(WorkoutSession session) {
        return new DashboardMostRecentCompletedResponse(
                session.getId(),
                session.getName(),
                session.getCompletedAt()
        );
    }
}
