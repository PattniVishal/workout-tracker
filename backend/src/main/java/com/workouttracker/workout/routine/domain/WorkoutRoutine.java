package com.workouttracker.workout.routine.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workout_routine")
public class WorkoutRoutine {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @OneToMany(mappedBy = "workoutRoutine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<RoutineExercise> exercises = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkoutRoutine() {
    }

    public static WorkoutRoutine create(UUID userId, String name, String description) {
        WorkoutRoutine routine = new WorkoutRoutine();
        routine.userId = userId;
        routine.name = name;
        routine.description = description;
        return routine;
    }

    public void updateDetails(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void clearExercises() {
        exercises.clear();
    }

    public void addExerciseSlots(List<RoutineExerciseSlot> slots) {
        for (RoutineExerciseSlot slot : slots) {
            exercises.add(RoutineExercise.create(this, slot.exerciseId(), slot.position(), slot.plannedSetCount()));
        }
    }

    public void replaceExercises(List<RoutineExerciseSlot> slots) {
        clearExercises();
        addExerciseSlots(slots);
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<RoutineExercise> getExercises() {
        return exercises;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
