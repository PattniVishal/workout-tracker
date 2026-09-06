package com.workouttracker.workout.session.application;

import com.workouttracker.auth.api.RegisterRequest;
import com.workouttracker.auth.api.UserResponse;
import com.workouttracker.auth.application.AuthService;
import com.workouttracker.common.security.UserPrincipal;
import com.workouttracker.exercise.api.CreateExerciseRequest;
import com.workouttracker.exercise.application.ExerciseService;
import com.workouttracker.workout.history.api.HistoryListResponse;
import com.workouttracker.workout.history.api.PreviousPerformanceResponse;
import com.workouttracker.workout.history.application.HistorySessionNotFoundException;
import com.workouttracker.workout.history.application.WorkoutHistoryService;
import com.workouttracker.workout.session.api.AddSessionExerciseRequest;
import com.workouttracker.workout.session.api.SessionResponse;
import com.workouttracker.workout.session.api.StartSessionRequest;
import com.workouttracker.workout.session.api.UpdateSetRequest;
import com.workouttracker.workout.session.domain.WorkoutSession;
import com.workouttracker.workout.session.domain.WorkoutSessionStatus;
import com.workouttracker.workout.session.infrastructure.WorkoutSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkoutHistoryServiceTest {

    private static final UUID BENCH_PRESS_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SQUAT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private WorkoutHistoryService workoutHistoryService;

    @Autowired
    private WorkoutSessionService workoutSessionService;

    @Autowired
    private WorkoutSessionRepository workoutSessionRepository;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private AuthService authService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listReturnsOnlyCompletedSessionsForAuthenticatedUser() {
        authenticateAs("User A", "history-service-a@example.com");
        UUID firstCompletedId = completeSession("First Workout");
        UUID secondCompletedId = completeSession("Second Workout");

        HistoryListResponse history = workoutHistoryService.listCompleted();

        assertThat(history.workouts()).hasSize(2);
        assertThat(history.workouts().get(0).id()).isEqualTo(secondCompletedId);
        assertThat(history.workouts().get(1).id()).isEqualTo(firstCompletedId);
        assertThat(history.workouts().get(0).completedSetCount()).isEqualTo(1);
        assertThat(history.workouts().get(0).exerciseCount()).isEqualTo(1);
        assertThat(history.workouts().get(0).durationSeconds()).isNotNull();
    }

    @Test
    void listExcludesInProgressSessions() {
        authenticateAs("Vishal", "history-service-in-progress@example.com");
        completeSession("Completed");
        workoutSessionService.start(new StartSessionRequest(null, "Active"));

        HistoryListResponse history = workoutHistoryService.listCompleted();

        assertThat(history.workouts()).hasSize(1);
        assertThat(history.workouts().getFirst().name()).isEqualTo("Completed");
    }

    @Test
    void listExcludesOtherUsersSessions() {
        UserResponse userA = authenticateAs("User A", "history-service-list-a@example.com");
        UUID userACompletedId = completeSession("User A Workout");

        authenticateAs("User B", "history-service-list-b@example.com");

        assertThat(workoutHistoryService.listCompleted().workouts()).isEmpty();

        setAuthentication(userA);
        assertThat(workoutHistoryService.listCompleted().workouts())
                .extracting(summary -> summary.id())
                .containsExactly(userACompletedId);
    }

    @Test
    void getReturnsCompletedSessionDetailWithSnapshots() {
        authenticateAs("Vishal", "history-service-detail@example.com");
        UUID customExerciseId = exerciseService.createCustom(new CreateExerciseRequest(
                "Snapshot Name",
                "Chest",
                List.of(),
                "Machine"
        )).id();
        UUID completedId = completeSessionWithExercise(customExerciseId, "Snapshot Name");

        exerciseService.updateCustom(customExerciseId, new CreateExerciseRequest(
                "Renamed Exercise",
                "Chest",
                List.of(),
                "Machine"
        ));

        SessionResponse detail = workoutHistoryService.getCompleted(completedId);

        assertThat(detail.status()).isEqualTo("COMPLETED");
        assertThat(detail.completedAt()).isNotNull();
        assertThat(detail.durationSeconds()).isNotNull();
        assertThat(detail.exercises()).hasSize(1);
        assertThat(detail.exercises().getFirst().exerciseName()).isEqualTo("Snapshot Name");
        assertThat(detail.exercises().getFirst().position()).isEqualTo(1);
        assertThat(detail.exercises().getFirst().sets().getFirst().setNumber()).isEqualTo(1);
    }

    @Test
    void getRejectsInProgressSession() {
        authenticateAs("Vishal", "history-service-in-progress-detail@example.com");
        UUID inProgressId = workoutSessionService.start(new StartSessionRequest(null, "Active")).id();

        assertThatThrownBy(() -> workoutHistoryService.getCompleted(inProgressId))
                .isInstanceOf(HistorySessionNotFoundException.class);
    }

    @Test
    void getRejectsAnotherUsersSession() {
        authenticateAs("User A", "history-service-detail-a@example.com");
        UUID completedId = completeSession("User A");

        authenticateAs("User B", "history-service-detail-b@example.com");

        assertThatThrownBy(() -> workoutHistoryService.getCompleted(completedId))
                .isInstanceOf(HistorySessionNotFoundException.class);
    }

    @Test
    void deleteRemovesCompletedSessionAndChildren() {
        authenticateAs("Vishal", "history-service-delete@example.com");
        UUID completedId = completeSession("Delete Me");

        workoutHistoryService.deleteCompleted(completedId);

        assertThat(workoutSessionRepository.findById(completedId)).isEmpty();
        assertThat(workoutHistoryService.listCompleted().workouts()).isEmpty();
    }

    @Test
    void deleteRejectsInProgressSession() {
        authenticateAs("Vishal", "history-service-delete-in-progress@example.com");
        UUID inProgressId = workoutSessionService.start(new StartSessionRequest(null, "Active")).id();

        assertThatThrownBy(() -> workoutHistoryService.deleteCompleted(inProgressId))
                .isInstanceOf(HistorySessionNotFoundException.class);

        assertThat(workoutSessionRepository.findById(inProgressId)).isPresent();
    }

    @Test
    void deleteRejectsAnotherUsersSession() {
        authenticateAs("User A", "history-service-delete-a@example.com");
        UUID completedId = completeSession("User A");

        authenticateAs("User B", "history-service-delete-b@example.com");

        assertThatThrownBy(() -> workoutHistoryService.deleteCompleted(completedId))
                .isInstanceOf(HistorySessionNotFoundException.class);

        WorkoutSession persisted = workoutSessionRepository.findById(completedId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(WorkoutSessionStatus.COMPLETED);
    }

    @Test
    void previousPerformanceReturnsLatestCompletedWorkout() {
        authenticateAs("Vishal", "history-service-prev-latest@example.com");
        completeSessionWithWeight("Older", new BigDecimal("40"));
        UUID latestSessionId = completeSessionWithWeight("Newer", new BigDecimal("55"));

        PreviousPerformanceResponse performance = workoutHistoryService
                .getPreviousPerformance(BENCH_PRESS_ID)
                .orElseThrow();

        assertThat(performance.sessionId()).isEqualTo(latestSessionId);
        assertThat(performance.exerciseId()).isEqualTo(BENCH_PRESS_ID);
        assertThat(performance.exerciseName()).isEqualTo("Bench Press");
        assertThat(performance.completedAt()).isNotNull();
        assertThat(performance.sets()).hasSize(1);
        assertThat(performance.sets().getFirst().weightKg()).isEqualByComparingTo("55");
        assertThat(performance.sets().getFirst().repetitions()).isEqualTo(10);
        assertThat(performance.sets().getFirst().setNumber()).isEqualTo(1);
    }

    @Test
    void previousPerformancePreservesSnapshotAfterCatalogRename() {
        authenticateAs("Vishal", "history-service-prev-snapshot@example.com");
        UUID customExerciseId = exerciseService.createCustom(new CreateExerciseRequest(
                "Snapshot Name",
                "Chest",
                List.of(),
                "Machine"
        )).id();
        completeSessionWithExercise(customExerciseId, "Snapshot Name");

        exerciseService.updateCustom(customExerciseId, new CreateExerciseRequest(
                "Renamed Exercise",
                "Chest",
                List.of(),
                "Machine"
        ));

        PreviousPerformanceResponse performance = workoutHistoryService
                .getPreviousPerformance(customExerciseId)
                .orElseThrow();

        assertThat(performance.exerciseName()).isEqualTo("Snapshot Name");
    }

    @Test
    void previousPerformanceReturnsOnlyCompletedSets() {
        authenticateAs("Vishal", "history-service-prev-sets@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "Partial"));
        SessionResponse withExercise = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 2));
        UUID workoutExerciseId = withExercise.exercises().getFirst().id();
        UUID firstSetId = withExercise.exercises().getFirst().sets().get(0).id();
        UUID secondSetId = withExercise.exercises().getFirst().sets().get(1).id();
        workoutSessionService.updateSet(
                workoutExerciseId,
                firstSetId,
                new UpdateSetRequest(new BigDecimal("50"), 10, true)
        );
        workoutSessionService.updateSet(
                workoutExerciseId,
                secondSetId,
                new UpdateSetRequest(new BigDecimal("45"), 8, false)
        );
        workoutSessionService.complete();

        PreviousPerformanceResponse performance = workoutHistoryService
                .getPreviousPerformance(BENCH_PRESS_ID)
                .orElseThrow();

        assertThat(performance.sets()).hasSize(1);
        assertThat(performance.sets().getFirst().setNumber()).isEqualTo(1);
        assertThat(performance.sets().getFirst().weightKg()).isEqualByComparingTo("50");
    }

    @Test
    void previousPerformanceReturnsEmptyWhenExerciseHasNoCompletedSets() {
        authenticateAs("Vishal", "history-service-prev-no-completed-sets@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "Mixed"));
        SessionResponse withExercises = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 1));
        UUID benchExerciseId = withExercises.exercises().getFirst().id();
        UUID benchSetId = withExercises.exercises().getFirst().sets().getFirst().id();
        workoutSessionService.updateSet(
                benchExerciseId,
                benchSetId,
                new UpdateSetRequest(new BigDecimal("50"), 10, false)
        );

        SessionResponse withSquat = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(SQUAT_ID, 1));
        UUID squatExerciseId = withSquat.exercises().get(1).id();
        UUID squatSetId = withSquat.exercises().get(1).sets().getFirst().id();
        workoutSessionService.updateSet(
                squatExerciseId,
                squatSetId,
                new UpdateSetRequest(new BigDecimal("100"), 5, true)
        );
        workoutSessionService.complete();

        assertThat(workoutHistoryService.getPreviousPerformance(BENCH_PRESS_ID)).isEmpty();
        assertThat(workoutHistoryService.getPreviousPerformance(SQUAT_ID)).isPresent();
    }

    @Test
    void previousPerformanceIgnoresInProgressSessions() {
        authenticateAs("Vishal", "history-service-prev-in-progress@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "Active"));
        workoutSessionService.addExercise(new AddSessionExerciseRequest(BENCH_PRESS_ID, 1));

        assertThat(workoutHistoryService.getPreviousPerformance(BENCH_PRESS_ID)).isEmpty();
    }

    @Test
    void previousPerformanceIgnoresOtherUsersSessions() {
        authenticateAs("User A", "history-service-prev-a@example.com");
        completeSession("User A");

        authenticateAs("User B", "history-service-prev-b@example.com");

        assertThat(workoutHistoryService.getPreviousPerformance(BENCH_PRESS_ID)).isEmpty();
    }

    @Test
    void previousPerformanceReturnsEmptyWhenExerciseNeverPerformed() {
        authenticateAs("Vishal", "history-service-prev-never@example.com");
        completeSession("Other exercise only");

        assertThat(workoutHistoryService.getPreviousPerformance(SQUAT_ID)).isEmpty();
    }

    @Test
    void previousPerformanceReturnsEmptyForUnknownExerciseId() {
        authenticateAs("Vishal", "history-service-prev-unknown@example.com");

        assertThat(workoutHistoryService.getPreviousPerformance(UUID.randomUUID())).isEmpty();
    }

    private UUID completeSessionWithWeight(String name, BigDecimal weight) {
        workoutSessionService.start(new StartSessionRequest(null, name));
        SessionResponse withExercise = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 1));
        UUID workoutExerciseId = withExercise.exercises().getFirst().id();
        UUID setId = withExercise.exercises().getFirst().sets().getFirst().id();
        workoutSessionService.updateSet(
                workoutExerciseId,
                setId,
                new UpdateSetRequest(weight, 10, true)
        );
        return workoutSessionService.complete().id();
    }

    private UUID completeSession(String name) {
        return completeSessionWithExercise(BENCH_PRESS_ID, "Bench Press", name);
    }

    private UUID completeSessionWithExercise(UUID exerciseId, String exerciseName) {
        return completeSessionWithExercise(exerciseId, exerciseName, "Workout");
    }

    private UUID completeSessionWithExercise(UUID exerciseId, String exerciseName, String sessionName) {
        workoutSessionService.start(new StartSessionRequest(null, sessionName));
        SessionResponse withExercise = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(exerciseId, 1));
        UUID workoutExerciseId = withExercise.exercises().getFirst().id();
        UUID setId = withExercise.exercises().getFirst().sets().getFirst().id();
        workoutSessionService.updateSet(
                workoutExerciseId,
                setId,
                new UpdateSetRequest(new BigDecimal("50"), 10, true)
        );
        return workoutSessionService.complete().id();
    }

    private UserResponse authenticateAs(String displayName, String email) {
        UserResponse registered = authService.register(
                new RegisterRequest(displayName, email, "secret-password"));
        setAuthentication(registered);
        return registered;
    }

    private void setAuthentication(UserResponse registered) {
        UserPrincipal principal = new UserPrincipal(
                registered.id(),
                registered.displayName(),
                registered.email(),
                "hashed-password"
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
