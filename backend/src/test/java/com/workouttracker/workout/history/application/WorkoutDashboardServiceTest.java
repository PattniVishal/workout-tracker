package com.workouttracker.workout.history.application;

import com.workouttracker.auth.api.RegisterRequest;
import com.workouttracker.auth.api.UserResponse;
import com.workouttracker.auth.application.AuthService;
import com.workouttracker.common.security.UserPrincipal;
import com.workouttracker.workout.history.api.DashboardResponse;
import com.workouttracker.workout.routine.api.SaveRoutineRequest;
import com.workouttracker.workout.routine.application.WorkoutRoutineService;
import com.workouttracker.workout.session.api.AddSessionExerciseRequest;
import com.workouttracker.workout.session.api.SessionResponse;
import com.workouttracker.workout.session.api.StartSessionRequest;
import com.workouttracker.workout.session.api.UpdateSetRequest;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkoutDashboardServiceTest {

    private static final UUID BENCH_PRESS_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private WorkoutDashboardService workoutDashboardService;

    @Autowired
    private WorkoutRoutineService workoutRoutineService;

    @Autowired
    private WorkoutSessionService workoutSessionService;

    @Autowired
    private AuthService authService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsEmptyDashboardForNewUser() {
        authenticateAs("Vishal", "dashboard-empty@example.com");

        DashboardResponse dashboard = workoutDashboardService.getDashboard();

        assertThat(dashboard.displayName()).isEqualTo("Vishal");
        assertThat(dashboard.hasActiveSession()).isFalse();
        assertThat(dashboard.activeSessionId()).isNull();
        assertThat(dashboard.routines()).isEmpty();
        assertThat(dashboard.mostRecentCompleted()).isNull();
        assertThat(dashboard.totalCompletedWorkouts()).isZero();
        assertThat(dashboard.totalCompletedSets()).isZero();
    }

    @Test
    void includesRoutineSummaries() {
        authenticateAs("Vishal", "dashboard-routines@example.com");
        UUID routineId = workoutRoutineService.create(new SaveRoutineRequest(
                "Push Day",
                "Chest",
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(BENCH_PRESS_ID, 3))
        )).id();

        DashboardResponse dashboard = workoutDashboardService.getDashboard();

        assertThat(dashboard.routines()).hasSize(1);
        assertThat(dashboard.routines().getFirst().id()).isEqualTo(routineId);
        assertThat(dashboard.routines().getFirst().name()).isEqualTo("Push Day");
        assertThat(dashboard.routines().getFirst().exerciseCount()).isEqualTo(1);
    }

    @Test
    void reflectsActiveSession() {
        authenticateAs("Vishal", "dashboard-active@example.com");
        UUID activeSessionId = workoutSessionService.start(new StartSessionRequest(null, "Active")).id();

        DashboardResponse dashboard = workoutDashboardService.getDashboard();

        assertThat(dashboard.hasActiveSession()).isTrue();
        assertThat(dashboard.activeSessionId()).isEqualTo(activeSessionId);
    }

    @Test
    void includesCompletedHistoryTotalsAndMostRecent() {
        authenticateAs("Vishal", "dashboard-history@example.com");
        completeSessionWithCompletedSet("Older Workout", new BigDecimal("40"));
        completeSessionWithTwoCompletedSets("Newer Workout");

        DashboardResponse dashboard = workoutDashboardService.getDashboard();

        assertThat(dashboard.totalCompletedWorkouts()).isEqualTo(2);
        assertThat(dashboard.totalCompletedSets()).isEqualTo(3);
        assertThat(dashboard.mostRecentCompleted()).isNotNull();
        assertThat(dashboard.mostRecentCompleted().name()).isEqualTo("Newer Workout");
        assertThat(dashboard.mostRecentCompleted().completedAt()).isNotNull();
    }

    @Test
    void doesNotExposeAnotherUsersData() {
        UserResponse userA = authenticateAs("User A", "dashboard-user-a@example.com");
        workoutRoutineService.create(new SaveRoutineRequest(
                "User A Routine",
                null,
                List.of(new SaveRoutineRequest.RoutineExerciseRequest(BENCH_PRESS_ID, 2))
        ));
        completeSessionWithCompletedSet("User A Workout", new BigDecimal("50"));

        authenticateAs("User B", "dashboard-user-b@example.com");

        DashboardResponse dashboard = workoutDashboardService.getDashboard();

        assertThat(dashboard.displayName()).isEqualTo("User B");
        assertThat(dashboard.routines()).isEmpty();
        assertThat(dashboard.mostRecentCompleted()).isNull();
        assertThat(dashboard.totalCompletedWorkouts()).isZero();
        assertThat(dashboard.totalCompletedSets()).isZero();

        setAuthentication(userA);
        assertThat(workoutDashboardService.getDashboard().totalCompletedWorkouts()).isEqualTo(1);
    }

    private void completeSessionWithCompletedSet(String name, BigDecimal weight) {
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
        workoutSessionService.complete();
    }

    private void completeSessionWithTwoCompletedSets(String name) {
        workoutSessionService.start(new StartSessionRequest(null, name));
        SessionResponse withExercise = workoutSessionService.addExercise(
                new AddSessionExerciseRequest(BENCH_PRESS_ID, 2));
        UUID workoutExerciseId = withExercise.exercises().getFirst().id();
        UUID firstSetId = withExercise.exercises().getFirst().sets().get(0).id();
        UUID secondSetId = withExercise.exercises().getFirst().sets().get(1).id();
        workoutSessionService.updateSet(
                workoutExerciseId,
                firstSetId,
                new UpdateSetRequest(new BigDecimal("50"), 8, true)
        );
        workoutSessionService.updateSet(
                workoutExerciseId,
                secondSetId,
                new UpdateSetRequest(new BigDecimal("55"), 6, true)
        );
        workoutSessionService.complete();
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
