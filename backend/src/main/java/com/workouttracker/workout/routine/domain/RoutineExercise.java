package com.workouttracker.workout.routine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "routine_exercise")
public class RoutineExercise {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_routine_id", nullable = false)
    private WorkoutRoutine workoutRoutine;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(nullable = false)
    private int position;

    @Column(name = "planned_set_count", nullable = false)
    private int plannedSetCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RoutineExercise() {
    }

    static RoutineExercise create(
            WorkoutRoutine workoutRoutine,
            UUID exerciseId,
            int position,
            int plannedSetCount
    ) {
        RoutineExercise routineExercise = new RoutineExercise();
        routineExercise.workoutRoutine = workoutRoutine;
        routineExercise.exerciseId = exerciseId;
        routineExercise.position = position;
        routineExercise.plannedSetCount = plannedSetCount;
        return routineExercise;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getExerciseId() {
        return exerciseId;
    }

    public int getPosition() {
        return position;
    }

    public int getPlannedSetCount() {
        return plannedSetCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
