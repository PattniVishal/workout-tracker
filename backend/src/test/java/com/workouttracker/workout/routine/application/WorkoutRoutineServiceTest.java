package com.workouttracker.workout.routine.application;

import com.workouttracker.auth.api.RegisterRequest;
import com.workouttracker.auth.api.UserResponse;
import com.workouttracker.auth.application.AuthService;
import com.workouttracker.common.security.UserPrincipal;
import com.workouttracker.exercise.api.CreateExerciseRequest;
import com.workouttracker.exercise.application.ExerciseService;
import com.workouttracker.workout.routine.api.RoutineDetailResponse;
import com.workouttracker.workout.routine.api.RoutineListResponse;
import com.workouttracker.workout.routine.api.SaveRoutineRequest;
import com.workouttracker.workout.routine.infrastructure.WorkoutRoutineRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkoutRoutineServiceTest {

    private static final UUID BENCH_PRESS_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private WorkoutRoutineService workoutRoutineService;

    @Autowired
    private WorkoutRoutineRepository workoutRoutineRepository;

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private AuthService authService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsRoutineWithServerDerivedOwnership() {
        authenticateAs("Vishal", "routine-service-create@example.com");

        RoutineDetailResponse created = workoutRoutineService.create(new SaveRoutineRequest(
                "Push Day",
                "Chest and triceps",
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(BENCH_PRESS_ID, 3))
        ));

        assertThat(created.name()).isEqualTo("Push Day");
        assertThat(created.exercises()).hasSize(1);
        assertThat(created.exercises().getFirst().exerciseId()).isEqualTo(BENCH_PRESS_ID);
        assertThat(created.exercises().getFirst().exerciseName()).isEqualTo("Bench Press");
        assertThat(created.exercises().getFirst().position()).isEqualTo(1);
        assertThat(workoutRoutineRepository.findById(created.id()).orElseThrow().getUserId()).isNotNull();
    }

    @Test
    void listsOnlyOwnRoutines() {
        UserResponse owner = authenticateAs("Owner", "routine-service-owner@example.com");
        RoutineDetailResponse ownerRoutine = workoutRoutineService.create(emptyRoutine("Owner Routine"));

        authenticateAs("Other", "routine-service-other@example.com");
        workoutRoutineService.create(emptyRoutine("Other Routine"));

        setAuthentication(owner);
        RoutineListResponse ownerList = workoutRoutineService.listByUser();

        assertThat(ownerList.routines()).hasSize(1);
        assertThat(ownerList.routines().getFirst().id()).isEqualTo(ownerRoutine.id());
        assertThat(ownerList.routines().getFirst().exerciseCount()).isZero();
    }

    @Test
    void rejectsDuplicateExerciseIds() {
        authenticateAs("Vishal", "routine-service-duplicate@example.com");

        assertThatThrownBy(() -> workoutRoutineService.create(new SaveRoutineRequest(
                "Push Day",
                null,
                List.of(
                        new SaveRoutineRequest.RoutineExerciseRequest(BENCH_PRESS_ID, 3),
                        new SaveRoutineRequest.RoutineExerciseRequest(BENCH_PRESS_ID, 4)
                )
        ))).isInstanceOf(DuplicateRoutineExerciseException.class);
    }

    @Test
    void rejectsNonPickableExercise() {
        authenticateAs("Owner", "routine-service-nonpickable@example.com");
        UUID customExerciseId = exerciseService.createCustom(new CreateExerciseRequest(
                "Archived Custom",
                "Chest",
                List.of(),
                "Machine"
        )).id();
        exerciseService.archiveCustom(customExerciseId);

        assertThatThrownBy(() -> workoutRoutineService.create(new SaveRoutineRequest(
                "Push Day",
                null,
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(customExerciseId, 3))
        ))).isInstanceOf(com.workouttracker.exercise.application.ExerciseNotPickableException.class);
    }

    @Test
    void updateReplacesEntireExerciseList() {
        authenticateAs("Vishal", "routine-service-update@example.com");
        RoutineDetailResponse created = workoutRoutineService.create(new SaveRoutineRequest(
                "Push Day",
                "Initial",
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(BENCH_PRESS_ID, 3))
        ));

        UUID squatId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        RoutineDetailResponse updated = workoutRoutineService.update(created.id(), new SaveRoutineRequest(
                "Leg Day",
                "Updated",
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(squatId, 5))
        ));

        assertThat(updated.name()).isEqualTo("Leg Day");
        assertThat(updated.description()).isEqualTo("Updated");
        assertThat(updated.exercises()).hasSize(1);
        assertThat(updated.exercises().getFirst().exerciseId()).isEqualTo(squatId);
        assertThat(updated.exercises().getFirst().position()).isEqualTo(1);
    }

    @Test
    void otherUserCannotUpdateRoutine() {
        authenticateAs("Owner", "routine-service-update-owner@example.com");
        RoutineDetailResponse created = workoutRoutineService.create(emptyRoutine("Owner Routine"));

        authenticateAs("Other", "routine-service-update-other@example.com");

        assertThatThrownBy(() -> workoutRoutineService.update(
                created.id(),
                emptyRoutine("Hijacked")
        )).isInstanceOf(RoutineNotFoundException.class);
    }

    @Test
    void deleteRemovesRoutineWithoutDeletingExercises() {
        authenticateAs("Vishal", "routine-service-delete@example.com");
        RoutineDetailResponse created = workoutRoutineService.create(new SaveRoutineRequest(
                "Delete Me",
                null,
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(BENCH_PRESS_ID, 3))
        ));

        workoutRoutineService.delete(created.id());

        assertThat(workoutRoutineRepository.findById(created.id())).isEmpty();
        assertThatThrownBy(() -> workoutRoutineService.getById(created.id()))
                .isInstanceOf(RoutineNotFoundException.class);
    }

    private SaveRoutineRequest emptyRoutine(String name) {
        return new SaveRoutineRequest(name, null, List.of());
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
