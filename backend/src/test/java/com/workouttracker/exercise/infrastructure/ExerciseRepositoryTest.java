package com.workouttracker.exercise.infrastructure;

import com.workouttracker.exercise.domain.Exercise;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExerciseRepositoryTest {

    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Test
    void flywayV2SeedsSystemExercises() {
        assertThat(exerciseRepository.count()).isGreaterThanOrEqualTo(6);

        Exercise benchPress = exerciseRepository
                .findById(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .orElseThrow();

        assertThat(benchPress.getName()).isEqualTo("Bench Press");
        assertThat(benchPress.getPrimaryMuscleGroup()).isEqualTo("Chest");
        assertThat(benchPress.getSecondaryMuscleGroups()).containsExactly("Triceps", "Shoulders");
        assertThat(benchPress.getCategory()).isEqualTo("Barbell");
        assertThat(benchPress.getCreatedByUserId()).isNull();
        assertThat(benchPress.getArchivedAt()).isNull();
    }

    @Test
    void findPickableReturnsSystemExercisesForAnyUser() {
        assertThat(exerciseRepository.findPickable(SYSTEM_USER_ID, "", ""))
                .hasSize(6)
                .extracting(Exercise::getName)
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
    void findPickableWithNullLikeFiltersUsesEmptyStringSentinel() {
        assertThat(exerciseRepository.findPickable(SYSTEM_USER_ID, "", ""))
                .hasSize(6);
    }

    @Test
    void findPickableFiltersByNameSubstring() {
        assertThat(exerciseRepository.findPickable(SYSTEM_USER_ID, "press", ""))
                .extracting(Exercise::getName)
                .containsExactly("Bench Press", "Overhead Press");
    }

    @Test
    void findPickableFiltersByNameCaseInsensitively() {
        assertThat(exerciseRepository.findPickable(SYSTEM_USER_ID, "BENCH", ""))
                .extracting(Exercise::getName)
                .containsExactly("Bench Press");
    }

    @Test
    void findPickableFiltersByPrimaryMuscleGroup() {
        assertThat(exerciseRepository.findPickable(SYSTEM_USER_ID, "", "Back"))
                .extracting(Exercise::getName)
                .containsExactly("Barbell Row", "Deadlift", "Pull-Up");
    }

    @Test
    void findPickableFiltersByQueryAndMuscleGroupTogether() {
        assertThat(exerciseRepository.findPickable(SYSTEM_USER_ID, "press", "Chest"))
                .extracting(Exercise::getName)
                .containsExactly("Bench Press");
    }
}
