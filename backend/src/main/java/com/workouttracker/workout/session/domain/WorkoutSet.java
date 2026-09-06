package com.workouttracker.workout.session.domain;

import com.workouttracker.workout.session.application.InvalidSetUpdateException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workout_set")
public class WorkoutSet {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_exercise_id", nullable = false)
    private WorkoutExercise workoutExercise;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "weight_kg", precision = 8, scale = 2)
    private BigDecimal weightKg;

    @Column
    private Integer repetitions;

    @Column(name = "is_completed", nullable = false)
    private boolean completed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkoutSet() {
    }

    static WorkoutSet create(WorkoutExercise workoutExercise, int setNumber) {
        WorkoutSet workoutSet = new WorkoutSet();
        workoutSet.workoutExercise = workoutExercise;
        workoutSet.setNumber = setNumber;
        workoutSet.completed = false;
        return workoutSet;
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

    public int getSetNumber() {
        return setNumber;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public Integer getRepetitions() {
        return repetitions;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    void applyUpdate(BigDecimal weightKg, Integer repetitions, Boolean completed) {
        if (weightKg != null) {
            if (weightKg.signum() < 0) {
                throw new InvalidSetUpdateException("Weight cannot be negative.");
            }
            this.weightKg = weightKg;
        }
        if (repetitions != null) {
            if (repetitions < 0) {
                throw new InvalidSetUpdateException("Repetitions cannot be negative.");
            }
            this.repetitions = repetitions;
        }
        if (completed != null) {
            if (completed && (this.weightKg == null || this.repetitions == null)) {
                throw new InvalidSetUpdateException("Completed set requires weight and repetitions.");
            }
            this.completed = completed;
        }
    }

    void setSetNumber(int setNumber) {
        this.setNumber = setNumber;
    }
}
