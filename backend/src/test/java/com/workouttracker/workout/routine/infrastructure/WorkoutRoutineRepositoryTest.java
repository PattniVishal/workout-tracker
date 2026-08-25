package com.workouttracker.workout.routine.infrastructure;

import com.workouttracker.auth.application.EmailNormalizer;
import com.workouttracker.auth.domain.User;
import com.workouttracker.auth.infrastructure.UserRepository;
import com.workouttracker.workout.routine.domain.RoutineExerciseSlot;
import com.workouttracker.workout.routine.domain.WorkoutRoutine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkoutRoutineRepositoryTest {

    private static final UUID BENCH_PRESS_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SQUAT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private WorkoutRoutineRepository workoutRoutineRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void setUpUser() {
        User user = userRepository.saveAndFlush(
                new User("Vishal", EmailNormalizer.normalize("routine-repo@example.com"), "$2a$10$hash"));
        userId = user.getId();
    }

    @Test
    void persistsRoutineWithOrderedExercises() {
        WorkoutRoutine routine = WorkoutRoutine.create(userId, "Push Day", "Chest focus");
        routine.replaceExercises(List.of(
                new RoutineExerciseSlot(SQUAT_ID, 2, 4),
                new RoutineExerciseSlot(BENCH_PRESS_ID, 1, 3)
        ));

        WorkoutRoutine saved = workoutRoutineRepository.saveAndFlush(routine);

        WorkoutRoutine loaded = workoutRoutineRepository.findDetailedByIdAndUserId(saved.getId(), userId)
                .orElseThrow();

        assertThat(loaded.getUserId()).isEqualTo(userId);
        assertThat(loaded.getName()).isEqualTo("Push Day");
        assertThat(loaded.getExercises().stream()
                .sorted(Comparator.comparingInt(exercise -> exercise.getPosition()))
                .map(exercise -> exercise.getExerciseId())
                .toList())
                .containsExactly(BENCH_PRESS_ID, SQUAT_ID);
        assertThat(loaded.getExercises().stream()
                .sorted(Comparator.comparingInt(exercise -> exercise.getPosition()))
                .map(exercise -> exercise.getPlannedSetCount())
                .toList())
                .containsExactly(3, 4);
    }

    @Test
    void deletesRoutineChildrenWhenRoutineIsDeleted() {
        WorkoutRoutine routine = WorkoutRoutine.create(userId, "Delete Me", null);
        routine.replaceExercises(List.of(new RoutineExerciseSlot(BENCH_PRESS_ID, 1, 3)));
        WorkoutRoutine saved = workoutRoutineRepository.saveAndFlush(routine);
        UUID routineId = saved.getId();
        UUID routineExerciseId = saved.getExercises().getFirst().getId();

        workoutRoutineRepository.deleteByIdAndUserId(routineId, userId);
        workoutRoutineRepository.flush();

        assertThat(workoutRoutineRepository.findById(routineId)).isEmpty();
        assertThat(workoutRoutineRepository.findDetailedByIdAndUserId(routineId, userId)).isEmpty();
        assertThat(routineExerciseId).isNotNull();
    }
}
