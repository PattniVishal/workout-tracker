package com.workouttracker.exercise.application;

import com.workouttracker.auth.api.RegisterRequest;
import com.workouttracker.auth.application.AuthService;
import com.workouttracker.auth.api.UserResponse;
import com.workouttracker.common.security.UserPrincipal;
import com.workouttracker.exercise.api.ExerciseListResponse;
import com.workouttracker.exercise.api.ExerciseResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExerciseServiceTest {

    @Autowired
    private ExerciseService exerciseService;

    @Autowired
    private AuthService authService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listPickableReturnsSeededSystemExercises() {
        authenticateAsRegisteredUser();

        ExerciseListResponse response = exerciseService.listPickable(null, null);

        assertThat(response.exercises()).hasSize(6);
        assertThat(response.exercises())
                .extracting(ExerciseResponse::name)
                .containsExactly(
                        "Barbell Row",
                        "Bench Press",
                        "Deadlift",
                        "Overhead Press",
                        "Pull-Up",
                        "Squat"
                );
    }

    @Test
    void listPickableFiltersByQueryAndMuscleGroup() {
        authenticateAsRegisteredUser();

        ExerciseListResponse response = exerciseService.listPickable("press", "Shoulders");

        assertThat(response.exercises())
                .extracting(ExerciseResponse::name)
                .containsExactly("Overhead Press");
    }

    @Test
    void listPickableTreatsNullAndBlankFiltersAsAbsent() {
        authenticateAsRegisteredUser();

        assertThat(exerciseService.listPickable(null, null).exercises()).hasSize(6);
        assertThat(exerciseService.listPickable("  ", null).exercises()).hasSize(6);
        assertThat(exerciseService.listPickable(null, "  ").exercises()).hasSize(6);
    }

    @Test
    void listPickableFiltersCaseInsensitively() {
        authenticateAsRegisteredUser();

        assertThat(exerciseService.listPickable("BENCH", null).exercises())
                .extracting(ExerciseResponse::name)
                .containsExactly("Bench Press");
    }

    @Test
    void listPickableFiltersByMuscleGroupOnly() {
        authenticateAsRegisteredUser();

        assertThat(exerciseService.listPickable(null, "Back").exercises())
                .extracting(ExerciseResponse::name)
                .containsExactly("Barbell Row", "Deadlift", "Pull-Up");
    }

    @Test
    void listPickableMapsSystemSource() {
        authenticateAsRegisteredUser();

        ExerciseResponse benchPress = exerciseService.listPickable("Bench", null).exercises().getFirst();

        assertThat(benchPress.id()).isEqualTo(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        assertThat(benchPress.primaryMuscleGroup()).isEqualTo("Chest");
        assertThat(benchPress.secondaryMuscleGroups()).containsExactly("Triceps", "Shoulders");
        assertThat(benchPress.category()).isEqualTo("Barbell");
        assertThat(benchPress.source()).isEqualTo("SYSTEM");
    }

    private void authenticateAsRegisteredUser() {
        UserResponse registered = authService.register(
                new RegisterRequest("Vishal", "exercise-service@example.com", "secret-password"));

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
