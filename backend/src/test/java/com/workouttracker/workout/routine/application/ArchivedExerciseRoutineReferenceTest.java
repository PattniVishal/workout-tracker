package com.workouttracker.workout.routine.application;

import com.workouttracker.auth.api.RegisterRequest;
import com.workouttracker.auth.api.UserResponse;
import com.workouttracker.auth.application.AuthService;
import com.workouttracker.common.security.UserPrincipal;
import com.workouttracker.exercise.api.CreateExerciseRequest;
import com.workouttracker.exercise.application.ExerciseNotFoundException;
import com.workouttracker.exercise.application.ExerciseNotPickableException;
import com.workouttracker.exercise.application.ExerciseService;
import com.workouttracker.workout.routine.api.RoutineDetailResponse;
import com.workouttracker.workout.routine.api.SaveRoutineRequest;
import com.workouttracker.workout.session.api.SessionResponse;
import com.workouttracker.workout.session.api.StartSessionRequest;
import com.workouttracker.workout.session.application.WorkoutSessionService;
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
class ArchivedExerciseRoutineReferenceTest {

    private static final UUID BENCH_PRESS_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private WorkoutRoutineService workoutRoutineService;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private WorkoutSessionService workoutSessionService;

    @Autowired
    private AuthService authService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void routineDetailRemainsAccessibleAfterExerciseArchive() {
        authenticateAs("Vishal", "archived-routine-detail@example.com");
        UUID customExerciseId = createCustomExercise("Archived Routine Exercise");
        RoutineDetailResponse created = workoutRoutineService.create(new SaveRoutineRequest(
                "Custom Day",
                null,
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(customExerciseId, 3))
        ));
        exerciseService.archiveCustom(customExerciseId);

        RoutineDetailResponse detail = workoutRoutineService.getById(created.id());

        assertThat(detail.exercises()).hasSize(1);
        assertThat(detail.exercises().getFirst().exerciseId()).isEqualTo(customExerciseId);
        assertThat(detail.exercises().getFirst().exerciseName()).isEqualTo("Archived Routine Exercise");
    }

    @Test
    void routineUpdateRetainsArchivedExerciseReference() {
        authenticateAs("Vishal", "archived-routine-update@example.com");
        UUID customExerciseId = createCustomExercise("Retained Archived Exercise");
        RoutineDetailResponse created = workoutRoutineService.create(new SaveRoutineRequest(
                "Custom Day",
                "Initial",
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(customExerciseId, 3))
        ));
        exerciseService.archiveCustom(customExerciseId);

        RoutineDetailResponse updated = workoutRoutineService.update(created.id(), new SaveRoutineRequest(
                "Custom Day Updated",
                "Updated",
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(customExerciseId, 4))
        ));

        assertThat(updated.name()).isEqualTo("Custom Day Updated");
        assertThat(updated.exercises()).hasSize(1);
        assertThat(updated.exercises().getFirst().exerciseId()).isEqualTo(customExerciseId);
        assertThat(updated.exercises().getFirst().plannedSetCount()).isEqualTo(4);
        assertThat(updated.exercises().getFirst().exerciseName()).isEqualTo("Retained Archived Exercise");
    }

    @Test
    void cannotAddArchivedExerciseToNewRoutine() {
        authenticateAs("Vishal", "archived-routine-create@example.com");
        UUID customExerciseId = createCustomExercise("Archived New Routine Exercise");
        exerciseService.archiveCustom(customExerciseId);

        assertThatThrownBy(() -> workoutRoutineService.create(new SaveRoutineRequest(
                "Should Fail",
                null,
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(customExerciseId, 3))
        ))).isInstanceOf(ExerciseNotPickableException.class);
    }

    @Test
    void cannotAddArchivedExerciseToExistingRoutineOnUpdate() {
        authenticateAs("Vishal", "archived-routine-update-new@example.com");
        RoutineDetailResponse created = workoutRoutineService.create(new SaveRoutineRequest(
                "Bench Day",
                null,
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(BENCH_PRESS_ID, 3))
        ));
        UUID archivedExerciseId = createCustomExercise("New Archived Exercise");
        exerciseService.archiveCustom(archivedExerciseId);

        assertThatThrownBy(() -> workoutRoutineService.update(created.id(), new SaveRoutineRequest(
                "Bench Day",
                null,
                List.of(
                        new SaveRoutineRequest.RoutineExerciseRequest(BENCH_PRESS_ID, 3),
                        new SaveRoutineRequest.RoutineExerciseRequest(archivedExerciseId, 2)
                )
        ))).isInstanceOf(ExerciseNotPickableException.class);
    }

    @Test
    void sessionStartFromRoutineWithArchivedExerciseSnapshotsName() {
        authenticateAs("Vishal", "archived-routine-start@example.com");
        UUID customExerciseId = createCustomExercise("Snapshot Archived Exercise");
        UUID routineId = workoutRoutineService.create(new SaveRoutineRequest(
                "Custom Day",
                null,
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(customExerciseId, 2))
        )).id();
        exerciseService.archiveCustom(customExerciseId);

        SessionResponse session = workoutSessionService.start(new StartSessionRequest(routineId, null));

        assertThat(session.exercises()).hasSize(1);
        assertThat(session.exercises().getFirst().exerciseId()).isEqualTo(customExerciseId);
        assertThat(session.exercises().getFirst().exerciseName()).isEqualTo("Snapshot Archived Exercise");
        assertThat(session.exercises().getFirst().sets()).hasSize(2);
    }

    @Test
    void archivedExerciseExcludedFromPickerList() {
        UserResponse user = authenticateAs("Vishal", "archived-routine-list@example.com");
        UUID customExerciseId = createCustomExercise("Hidden Archived Exercise");
        exerciseService.archiveCustom(customExerciseId);

        assertThat(exerciseService.listPickable(null, null).exercises())
                .extracting(exercise -> exercise.id())
                .doesNotContain(customExerciseId);

        assertThatThrownBy(() -> exerciseService.requirePickable(customExerciseId, user.id()))
                .isInstanceOf(ExerciseNotPickableException.class);
    }

    @Test
    void otherUserCannotResolveArchivedCustomReference() {
        authenticateAs("Owner", "archived-routine-owner@example.com");
        UUID customExerciseId = createCustomExercise("Owner Archived Exercise");
        exerciseService.archiveCustom(customExerciseId);

        authenticateAs("Other", "archived-routine-other@example.com");

        assertThatThrownBy(() -> exerciseService.requireResolvableReference(customExerciseId, currentUserId()))
                .isInstanceOf(ExerciseNotFoundException.class);
    }

    private UUID createCustomExercise(String name) {
        return exerciseService.createCustom(new CreateExerciseRequest(
                name,
                "Chest",
                List.of(),
                "Machine"
        )).id();
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
