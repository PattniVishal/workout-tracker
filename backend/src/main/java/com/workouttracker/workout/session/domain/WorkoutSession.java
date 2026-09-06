package com.workouttracker.workout.session.domain;

import com.workouttracker.workout.routine.domain.RoutineExercise;
import com.workouttracker.workout.routine.domain.WorkoutRoutine;
import com.workouttracker.workout.session.application.NoCompletedSetsException;
import com.workouttracker.workout.session.application.SessionNotInProgressException;
import com.workouttracker.workout.session.application.SessionResourceNotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Duration;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "workout_session")
public class WorkoutSession {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "origin_routine_id")
    private UUID originRoutineId;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkoutSessionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "workoutSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private Set<WorkoutExercise> exercises = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkoutSession() {
    }

    public static WorkoutSession startFromRoutine(
            UUID userId,
            WorkoutRoutine routine,
            Map<UUID, String> exerciseNamesById
    ) {
        WorkoutSession session = new WorkoutSession();
        session.userId = userId;
        session.originRoutineId = routine.getId();
        session.name = routine.getName();
        session.status = WorkoutSessionStatus.IN_PROGRESS;
        session.startedAt = Instant.now();

        routine.getExercises().stream()
                .sorted(Comparator.comparingInt(RoutineExercise::getPosition))
                .forEach(slot -> session.exercises.add(WorkoutExercise.create(
                        session,
                        slot.getExerciseId(),
                        exerciseNamesById.get(slot.getExerciseId()),
                        slot.getPosition(),
                        slot.getPlannedSetCount()
                )));

        return session;
    }

    public static WorkoutSession startEmpty(UUID userId, String name) {
        WorkoutSession session = new WorkoutSession();
        session.userId = userId;
        session.name = name;
        session.status = WorkoutSessionStatus.IN_PROGRESS;
        session.startedAt = Instant.now();
        return session;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOriginRoutineId() {
        return originRoutineId;
    }

    public String getName() {
        return name;
    }

    public WorkoutSessionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Set<WorkoutExercise> getExercises() {
        return exercises;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void assertInProgress() {
        if (status != WorkoutSessionStatus.IN_PROGRESS) {
            throw new SessionNotInProgressException();
        }
    }

    public void addExercise(UUID exerciseId, String exerciseName, int initialSetCount) {
        assertInProgress();
        int nextPosition = exercises.stream()
                .mapToInt(WorkoutExercise::getPosition)
                .max()
                .orElse(0) + 1;
        exercises.add(WorkoutExercise.create(this, exerciseId, exerciseName, nextPosition, initialSetCount));
    }

    public void removeExercise(UUID workoutExerciseId) {
        assertInProgress();
        WorkoutExercise exercise = findExercise(workoutExerciseId)
                .orElseThrow(SessionResourceNotFoundException::new);
        exercises.remove(exercise);
    }

    public void addSet(UUID workoutExerciseId) {
        assertInProgress();
        WorkoutExercise exercise = findExercise(workoutExerciseId)
                .orElseThrow(SessionResourceNotFoundException::new);
        exercise.addSet();
    }

    public void updateSet(
            UUID workoutExerciseId,
            UUID setId,
            BigDecimal weightKg,
            Integer repetitions,
            Boolean completed
    ) {
        assertInProgress();
        WorkoutExercise exercise = findExercise(workoutExerciseId)
                .orElseThrow(SessionResourceNotFoundException::new);
        WorkoutSet set = exercise.findSet(setId)
                .orElseThrow(SessionResourceNotFoundException::new);
        set.applyUpdate(weightKg, repetitions, completed);
    }

    public void removeSet(UUID workoutExerciseId, UUID setId) {
        assertInProgress();
        WorkoutExercise exercise = findExercise(workoutExerciseId)
                .orElseThrow(SessionResourceNotFoundException::new);
        exercise.removeSet(setId);
    }

    public void renumberSets(UUID workoutExerciseId) {
        assertInProgress();
        WorkoutExercise exercise = findExercise(workoutExerciseId)
                .orElseThrow(SessionResourceNotFoundException::new);
        exercise.renumberSets();
    }

    Optional<WorkoutExercise> findExercise(UUID workoutExerciseId) {
        return exercises.stream()
                .filter(exercise -> exercise.getId().equals(workoutExerciseId))
                .findFirst();
    }

    public void complete(Instant completedAt) {
        assertInProgress();
        if (!hasCompletedSet()) {
            throw new NoCompletedSetsException();
        }
        status = WorkoutSessionStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    public Long durationSeconds() {
        if (completedAt == null) {
            return null;
        }
        return Duration.between(startedAt, completedAt).getSeconds();
    }

    private boolean hasCompletedSet() {
        return exercises.stream()
                .flatMap(exercise -> exercise.getSets().stream())
                .anyMatch(WorkoutSet::isCompleted);
    }
}
