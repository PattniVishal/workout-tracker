package com.workouttracker.workout.session.application;

import com.workouttracker.auth.api.RegisterRequest;
import com.workouttracker.auth.api.UserResponse;
import com.workouttracker.auth.application.AuthService;
import com.workouttracker.common.security.UserPrincipal;
import com.workouttracker.exercise.api.CreateExerciseRequest;
import com.workouttracker.exercise.application.ExerciseService;
import com.workouttracker.workout.routine.api.SaveRoutineRequest;
import com.workouttracker.workout.routine.application.WorkoutRoutineService;
import com.workouttracker.workout.session.api.AddSessionExerciseRequest;
import com.workouttracker.workout.session.api.SessionResponse;
import com.workouttracker.workout.session.api.StartSessionRequest;
import com.workouttracker.workout.session.api.UpdateSetRequest;
import com.workouttracker.workout.session.domain.WorkoutSession;
import com.workouttracker.workout.session.domain.WorkoutSessionStatus;
import com.workouttracker.workout.session.infrastructure.WorkoutSessionRepository;
import com.workouttracker.workout.session.application.InvalidSetUpdateException;
import com.workouttracker.workout.session.application.NoCompletedSetsException;
import com.workouttracker.workout.session.application.NoCurrentSessionException;
import com.workouttracker.workout.session.application.SessionResourceNotFoundException;
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
class WorkoutSessionServiceTest {

    private static final UUID BENCH_PRESS_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private WorkoutSessionService workoutSessionService;

    @Autowired
    private WorkoutSessionRepository workoutSessionRepository;

    @Autowired
    private WorkoutRoutineService workoutRoutineService;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private AuthService authService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void startsEmptySessionForAuthenticatedUser() {
        authenticateAs("Vishal", "session-service-empty@example.com");

        SessionResponse session = workoutSessionService.start(new StartSessionRequest(null, "Ad hoc"));

        assertThat(session.status()).isEqualTo("IN_PROGRESS");
        assertThat(session.name()).isEqualTo("Ad hoc");
        assertThat(session.originRoutineId()).isNull();
        assertThat(session.exercises()).isEmpty();
        assertThat(session.durationSeconds()).isNull();
    }

    @Test
    void startsSessionFromRoutineWithSnapshotsAndPlannedSets() {
        authenticateAs("Vishal", "session-service-routine@example.com");
        UUID routineId = workoutRoutineService.create(new SaveRoutineRequest(
                "Push Day",
                null,
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(BENCH_PRESS_ID, 3))
        )).id();

        SessionResponse session = workoutSessionService.start(new StartSessionRequest(routineId, null));

        assertThat(session.name()).isEqualTo("Push Day");
        assertThat(session.originRoutineId()).isEqualTo(routineId);
        assertThat(session.exercises()).hasSize(1);
        assertThat(session.exercises().getFirst().exerciseName()).isEqualTo("Bench Press");
        assertThat(session.exercises().getFirst().sets()).hasSize(3);
        assertThat(session.exercises().getFirst().sets())
                .allMatch(set -> !set.completed() && set.weightKg() == null && set.repetitions() == null);
    }

    @Test
    void preservesExerciseNameSnapshotAfterCatalogRename() {
        authenticateAs("Vishal", "session-service-snapshot@example.com");
        UUID customExerciseId = exerciseService.createCustom(new CreateExerciseRequest(
                "Original Name",
                "Chest",
                List.of(),
                "Machine"
        )).id();
        UUID routineId = workoutRoutineService.create(new SaveRoutineRequest(
                "Custom Routine",
                null,
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(customExerciseId, 2))
        )).id();

        workoutSessionService.start(new StartSessionRequest(routineId, null));
        exerciseService.updateCustom(customExerciseId, new CreateExerciseRequest(
                "Renamed Exercise",
                "Chest",
                List.of(),
                "Machine"
        ));

        SessionResponse current = workoutSessionService.getInProgress().orElseThrow();

        assertThat(current.exercises().getFirst().exerciseName()).isEqualTo("Original Name");
    }

    @Test
    void rejectsSecondActiveSession() {
        authenticateAs("Vishal", "session-service-second@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "First"));

        assertThatThrownBy(() -> workoutSessionService.start(new StartSessionRequest(null, "Second")))
                .isInstanceOf(ActiveSessionExistsException.class);
    }

    @Test
    void rejectsInvalidStartRequest() {
        authenticateAs("Vishal", "session-service-invalid@example.com");

        assertThatThrownBy(() -> workoutSessionService.start(new StartSessionRequest(null, null)))
                .isInstanceOf(InvalidSessionStartRequestException.class);
        assertThatThrownBy(() -> workoutSessionService.start(new StartSessionRequest(BENCH_PRESS_ID, "Both")))
                .isInstanceOf(InvalidSessionStartRequestException.class);
    }

    @Test
    void getInProgressReturnsCurrentUsersSessionOnly() {
        UserResponse userA = authenticateAs("User A", "session-service-current-a@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "User A Session"));

        authenticateAs("User B", "session-service-current-b@example.com");

        assertThat(workoutSessionService.getInProgress()).isEmpty();

        setAuthentication(userA);
        SessionResponse current = workoutSessionService.getInProgress().orElseThrow();
        assertThat(current.name()).isEqualTo("User A Session");
    }

    @Test
    void addExerciseCreatesSnapshotAndInitialSets() {
        authenticateAs("Vishal", "session-service-add-exercise@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "Ad hoc"));

        SessionResponse session = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 2));

        assertThat(session.exercises()).hasSize(1);
        assertThat(session.exercises().getFirst().exerciseName()).isEqualTo("Bench Press");
        assertThat(session.exercises().getFirst().position()).isEqualTo(1);
        assertThat(session.exercises().getFirst().sets()).hasSize(2);
        assertThat(session.exercises().getFirst().sets())
                .allMatch(set -> !set.completed() && set.weightKg() == null && set.repetitions() == null);
    }

    @Test
    void allowsDuplicateExerciseOnSession() {
        authenticateAs("Vishal", "session-service-duplicate-exercise@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "Ad hoc"));

        workoutSessionService.addExercise(new AddSessionExerciseRequest(BENCH_PRESS_ID, 1));
        SessionResponse session = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 1));

        assertThat(session.exercises()).hasSize(2);
        assertThat(session.exercises().get(0).position()).isEqualTo(1);
        assertThat(session.exercises().get(1).position()).isEqualTo(2);
    }

    @Test
    void addExerciseWithoutActiveSessionFails() {
        authenticateAs("Vishal", "session-service-no-session-add@example.com");

        assertThatThrownBy(() -> workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 1)))
                .isInstanceOf(NoCurrentSessionException.class);
    }

    @Test
    void removeExerciseDeletesSets() {
        authenticateAs("Vishal", "session-service-remove-exercise@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "Ad hoc"));
        SessionResponse withExercise = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 3));
        UUID workoutExerciseId = withExercise.exercises().getFirst().id();

        SessionResponse session = workoutSessionService.removeExercise(workoutExerciseId);

        assertThat(session.exercises()).isEmpty();
        WorkoutSession persisted = workoutSessionRepository.findDetailedByUserIdAndStatus(
                currentUserId(), WorkoutSessionStatus.IN_PROGRESS
        ).orElseThrow();
        assertThat(persisted.getExercises()).isEmpty();
    }

    @Test
    void removeExerciseFromAnotherUsersSessionFails() {
        UserResponse userA = authenticateAs("User A", "session-service-remove-a@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "User A"));
        SessionResponse userASession = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 1));
        UUID workoutExerciseId = userASession.exercises().getFirst().id();

        authenticateAs("User B", "session-service-remove-b@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "User B"));

        assertThatThrownBy(() -> workoutSessionService.removeExercise(workoutExerciseId))
                .isInstanceOf(SessionResourceNotFoundException.class);

        setAuthentication(userA);
        assertThat(workoutSessionService.getInProgress().orElseThrow().exercises()).hasSize(1);
    }

    @Test
    void addSetAppendsNextSetNumber() {
        authenticateAs("Vishal", "session-service-add-set@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "Ad hoc"));
        UUID workoutExerciseId = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 1))
                .exercises().getFirst().id();

        SessionResponse session = workoutSessionService.addSet(workoutExerciseId);

        assertThat(session.exercises().getFirst().sets()).hasSize(2);
        assertThat(session.exercises().getFirst().sets().get(1).setNumber()).isEqualTo(2);
    }

    @Test
    void updateSetLogsWeightAndRepetitionsWithoutCompleting() {
        authenticateAs("Vishal", "session-service-update-set@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "Ad hoc"));
        SessionResponse withExercise = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 1));
        UUID workoutExerciseId = withExercise.exercises().getFirst().id();
        UUID setId = withExercise.exercises().getFirst().sets().getFirst().id();

        SessionResponse session = workoutSessionService.updateSet(
                workoutExerciseId,
                setId,
                new UpdateSetRequest(new BigDecimal("50.5"), 10, null)
        );

        assertThat(session.exercises().getFirst().sets().getFirst().weightKg()).isEqualByComparingTo("50.5");
        assertThat(session.exercises().getFirst().sets().getFirst().repetitions()).isEqualTo(10);
        assertThat(session.exercises().getFirst().sets().getFirst().completed()).isFalse();
    }

    @Test
    void updateSetCanMarkCompletedWhenWeightAndRepsPresent() {
        authenticateAs("Vishal", "session-service-complete-set@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "Ad hoc"));
        SessionResponse withExercise = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 1));
        UUID workoutExerciseId = withExercise.exercises().getFirst().id();
        UUID setId = withExercise.exercises().getFirst().sets().getFirst().id();

        SessionResponse session = workoutSessionService.updateSet(
                workoutExerciseId,
                setId,
                new UpdateSetRequest(new BigDecimal("40"), 12, true)
        );

        assertThat(session.exercises().getFirst().sets().getFirst().completed()).isTrue();
    }

    @Test
    void updateSetRejectsCompletedWithoutValues() {
        authenticateAs("Vishal", "session-service-complete-invalid@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "Ad hoc"));
        SessionResponse withExercise = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 1));
        UUID workoutExerciseId = withExercise.exercises().getFirst().id();
        UUID setId = withExercise.exercises().getFirst().sets().getFirst().id();

        assertThatThrownBy(() -> workoutSessionService.updateSet(
                workoutExerciseId,
                setId,
                new UpdateSetRequest(null, null, true)))
                .isInstanceOf(InvalidSetUpdateException.class);
    }

    @Test
    void removeSetRenumbersRemainingSets() {
        authenticateAs("Vishal", "session-service-remove-set@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "Ad hoc"));
        SessionResponse withExercise = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 3));
        UUID workoutExerciseId = withExercise.exercises().getFirst().id();
        UUID middleSetId = withExercise.exercises().getFirst().sets().get(1).id();

        SessionResponse session = workoutSessionService.removeSet(workoutExerciseId, middleSetId);

        assertThat(session.exercises().getFirst().sets()).hasSize(2);
        assertThat(session.exercises().getFirst().sets().get(0).setNumber()).isEqualTo(1);
        assertThat(session.exercises().getFirst().sets().get(1).setNumber()).isEqualTo(2);
    }

    @Test
    void removeSetFromWrongExerciseFails() {
        authenticateAs("Vishal", "session-service-wrong-exercise-set@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "Ad hoc"));
        SessionResponse first = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 1));
        SessionResponse second = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 1));
        UUID wrongWorkoutExerciseId = second.exercises().get(1).id();
        UUID setId = first.exercises().getFirst().sets().getFirst().id();

        assertThatThrownBy(() -> workoutSessionService.removeSet(wrongWorkoutExerciseId, setId))
                .isInstanceOf(SessionResourceNotFoundException.class);
    }

    @Test
    void completeTransitionsSessionToCompleted() {
        authenticateAs("Vishal", "session-service-complete@example.com");
        startSessionWithOneCompletedSet();

        SessionResponse completed = workoutSessionService.complete();

        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.completedAt()).isNotNull();
        assertThat(completed.durationSeconds()).isNotNull().isGreaterThanOrEqualTo(0L);
        assertThat(workoutSessionService.getInProgress()).isEmpty();
    }

    @Test
    void completeRejectsWhenNoSetsAreCompleted() {
        authenticateAs("Vishal", "session-service-complete-empty@example.com");
        workoutSessionService.start(new StartSessionRequest(null, "Ad hoc"));
        workoutSessionService.addExercise(new AddSessionExerciseRequest(BENCH_PRESS_ID, 1));

        assertThatThrownBy(() -> workoutSessionService.complete())
                .isInstanceOf(NoCompletedSetsException.class);

        assertThat(workoutSessionService.getInProgress()).isPresent();
    }

    @Test
    void completeWithoutActiveSessionFails() {
        authenticateAs("Vishal", "session-service-complete-none@example.com");

        assertThatThrownBy(() -> workoutSessionService.complete())
                .isInstanceOf(NoCurrentSessionException.class);
    }

    @Test
    void anotherUserCannotCompleteCurrentSession() {
        authenticateAs("User A", "session-service-complete-a@example.com");
        startSessionWithOneCompletedSet();

        authenticateAs("User B", "session-service-complete-b@example.com");

        assertThatThrownBy(() -> workoutSessionService.complete())
                .isInstanceOf(NoCurrentSessionException.class);
    }

    @Test
    void discardRemovesInProgressSession() {
        authenticateAs("Vishal", "session-service-discard@example.com");
        SessionResponse started = workoutSessionService.start(new StartSessionRequest(null, "Discard Me"));
        workoutSessionService.addExercise(new AddSessionExerciseRequest(BENCH_PRESS_ID, 2));
        UUID sessionId = started.id();

        workoutSessionService.discard();

        assertThat(workoutSessionRepository.findById(sessionId)).isEmpty();
        assertThat(workoutSessionService.getInProgress()).isEmpty();
    }

    @Test
    void discardWithoutActiveSessionFails() {
        authenticateAs("Vishal", "session-service-discard-none@example.com");

        assertThatThrownBy(() -> workoutSessionService.discard())
                .isInstanceOf(NoCurrentSessionException.class);
    }

    @Test
    void discardDoesNotDeleteCompletedSession() {
        authenticateAs("Vishal", "session-service-discard-completed@example.com");
        startSessionWithOneCompletedSet();
        UUID completedId = workoutSessionService.complete().id();

        assertThatThrownBy(() -> workoutSessionService.discard())
                .isInstanceOf(NoCurrentSessionException.class);

        WorkoutSession persisted = workoutSessionRepository.findById(completedId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(WorkoutSessionStatus.COMPLETED);
    }

    @Test
    void anotherUserCannotDiscardCurrentSession() {
        authenticateAs("User A", "session-service-discard-a@example.com");
        UUID sessionId = workoutSessionService.start(new StartSessionRequest(null, "User A")).id();

        authenticateAs("User B", "session-service-discard-b@example.com");

        assertThatThrownBy(() -> workoutSessionService.discard())
                .isInstanceOf(NoCurrentSessionException.class);

        assertThat(workoutSessionRepository.findById(sessionId)).isPresent();
    }

    private void startSessionWithOneCompletedSet() {
        workoutSessionService.start(new StartSessionRequest(null, "Ad hoc"));
        SessionResponse withExercise = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 1));
        UUID workoutExerciseId = withExercise.exercises().getFirst().id();
        UUID setId = withExercise.exercises().getFirst().sets().getFirst().id();
        workoutSessionService.updateSet(
                workoutExerciseId,
                setId,
                new UpdateSetRequest(new BigDecimal("50"), 10, true)
        );
    }

    private UUID currentUserId() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
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
