package com.workouttracker.workout.session.infrastructure;

import com.workouttracker.auth.application.EmailNormalizer;
import com.workouttracker.auth.domain.User;
import com.workouttracker.auth.infrastructure.UserRepository;
import com.workouttracker.workout.session.application.NoCompletedSetsException;
import com.workouttracker.workout.session.application.SessionResourceNotFoundException;
import com.workouttracker.workout.session.domain.WorkoutExercise;
import com.workouttracker.workout.session.domain.WorkoutSession;
import com.workouttracker.workout.session.domain.WorkoutSessionStatus;
import com.workouttracker.workout.session.domain.WorkoutSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkoutSessionRepositoryTest {

    @Autowired
    private WorkoutSessionRepository workoutSessionRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void setUpUser() {
        User user = userRepository.saveAndFlush(
                new User("Vishal", EmailNormalizer.normalize("session-repo@example.com"), "$2a$10$hash"));
        userId = user.getId();
    }

    @Test
    void flywayV4AllowsSingleInProgressSessionPerUser() {
        WorkoutSession first = WorkoutSession.startEmpty(userId, "First");
        workoutSessionRepository.saveAndFlush(first);

        assertThat(workoutSessionRepository.existsByUserIdAndStatus(userId, WorkoutSessionStatus.IN_PROGRESS))
                .isTrue();
        assertThat(workoutSessionRepository.findDetailedByUserIdAndStatus(userId, WorkoutSessionStatus.IN_PROGRESS))
                .isPresent();
    }

    @Test
    void persistsAddedExerciseAndSets() {
        WorkoutSession session = WorkoutSession.startEmpty(userId, "Logging");
        session.addExercise(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "Bench Press",
                2
        );
        workoutSessionRepository.saveAndFlush(session);

        WorkoutSession loaded = workoutSessionRepository
                .findDetailedByUserIdAndStatus(userId, WorkoutSessionStatus.IN_PROGRESS)
                .orElseThrow();

        assertThat(loaded.getExercises()).hasSize(1);
        WorkoutExercise exercise = loaded.getExercises().iterator().next();
        assertThat(exercise.getExerciseName()).isEqualTo("Bench Press");
        assertThat(exercise.getPosition()).isEqualTo(1);
        assertThat(exercise.getSets()).hasSize(2);
        assertThat(exercise.getSets())
                .extracting(WorkoutSet::getSetNumber)
                .containsExactly(1, 2);
    }

    @Test
    void removingExerciseCascadesSets() {
        WorkoutSession session = WorkoutSession.startEmpty(userId, "Logging");
        session.addExercise(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "Bench Press",
                2
        );
        UUID workoutExerciseId = workoutSessionRepository.saveAndFlush(session)
                .getExercises().iterator().next().getId();

        WorkoutSession loaded = workoutSessionRepository
                .findDetailedByUserIdAndStatus(userId, WorkoutSessionStatus.IN_PROGRESS)
                .orElseThrow();
        loaded.removeExercise(workoutExerciseId);
        workoutSessionRepository.saveAndFlush(loaded);

        WorkoutSession reloaded = workoutSessionRepository
                .findDetailedByUserIdAndStatus(userId, WorkoutSessionStatus.IN_PROGRESS)
                .orElseThrow();
        assertThat(reloaded.getExercises()).isEmpty();
    }

    @Test
    void setNumbersRemainUniqueAfterDeletion() {
        WorkoutSession session = WorkoutSession.startEmpty(userId, "Logging");
        session.addExercise(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "Bench Press",
                3
        );
        WorkoutSession saved = workoutSessionRepository.saveAndFlush(session);
        WorkoutExercise exercise = saved.getExercises().iterator().next();
        UUID middleSetId = exercise.getSets().stream()
                .filter(set -> set.getSetNumber() == 2)
                .findFirst()
                .orElseThrow()
                .getId();

        saved.removeSet(exercise.getId(), middleSetId);
        workoutSessionRepository.saveAndFlush(saved);
        saved.renumberSets(exercise.getId());
        workoutSessionRepository.saveAndFlush(saved);

        WorkoutSession reloaded = workoutSessionRepository
                .findDetailedByUserIdAndStatus(userId, WorkoutSessionStatus.IN_PROGRESS)
                .orElseThrow();
        WorkoutExercise reloadedExercise = reloaded.getExercises().iterator().next();
        assertThat(reloadedExercise.getSets())
                .extracting(WorkoutSet::getSetNumber)
                .containsExactly(1, 2);
    }

    @Test
    void removeSetThrowsWhenSetNotInExercise() {
        WorkoutSession session = WorkoutSession.startEmpty(userId, "Logging");
        session.addExercise(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "Bench Press",
                1
        );
        WorkoutSession saved = workoutSessionRepository.saveAndFlush(session);
        WorkoutExercise exercise = saved.getExercises().iterator().next();

        assertThatThrownBy(() -> saved.removeSet(exercise.getId(), UUID.randomUUID()))
                .isInstanceOf(SessionResourceNotFoundException.class);
    }

    @Test
    void completePersistsCompletedStatusAndTimestamp() {
        WorkoutSession session = WorkoutSession.startEmpty(userId, "Complete");
        session.addExercise(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "Bench Press",
                1
        );
        WorkoutSession saved = workoutSessionRepository.saveAndFlush(session);
        WorkoutExercise exercise = saved.getExercises().iterator().next();
        WorkoutSet set = exercise.getSets().iterator().next();
        saved.updateSet(exercise.getId(), set.getId(), new BigDecimal("40"), 8, true);

        Instant completedAt = saved.getStartedAt().plusSeconds(1800);
        saved.complete(completedAt);
        workoutSessionRepository.saveAndFlush(saved);

        WorkoutSession loaded = workoutSessionRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(WorkoutSessionStatus.COMPLETED);
        assertThat(loaded.getCompletedAt()).isEqualTo(completedAt);
        assertThat(loaded.durationSeconds()).isEqualTo(1800L);
        assertThat(workoutSessionRepository.findDetailedByUserIdAndStatus(userId, WorkoutSessionStatus.IN_PROGRESS))
                .isEmpty();
    }

    @Test
    void completeThrowsWhenNoCompletedSets() {
        WorkoutSession session = WorkoutSession.startEmpty(userId, "Empty complete");
        session.addExercise(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "Bench Press",
                1
        );
        WorkoutSession saved = workoutSessionRepository.saveAndFlush(session);

        assertThatThrownBy(() -> saved.complete(Instant.now()))
                .isInstanceOf(NoCompletedSetsException.class);
    }

    @Test
    void discardDeletesSessionAggregate() {
        WorkoutSession session = WorkoutSession.startEmpty(userId, "Discard");
        session.addExercise(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "Bench Press",
                2
        );
        UUID sessionId = workoutSessionRepository.saveAndFlush(session).getId();

        workoutSessionRepository.delete(session);
        workoutSessionRepository.flush();

        assertThat(workoutSessionRepository.findById(sessionId)).isEmpty();
    }
}
