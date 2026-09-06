package com.workouttracker.workout.session.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import com.workouttracker.workout.session.application.SessionResourceNotFoundException;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "workout_exercise")
public class WorkoutExercise {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_session_id", nullable = false)
    private WorkoutSession workoutSession;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "exercise_name", nullable = false, length = 200)
    private String exerciseName;

    @Column(nullable = false)
    private int position;

    @OneToMany(mappedBy = "workoutExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("setNumber ASC")
    private Set<WorkoutSet> sets = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WorkoutExercise() {
    }

    static WorkoutExercise create(
            WorkoutSession workoutSession,
            UUID exerciseId,
            String exerciseName,
            int position,
            int plannedSetCount
    ) {
        WorkoutExercise workoutExercise = new WorkoutExercise();
        workoutExercise.workoutSession = workoutSession;
        workoutExercise.exerciseId = exerciseId;
        workoutExercise.exerciseName = exerciseName;
        workoutExercise.position = position;

        for (int setNumber = 1; setNumber <= plannedSetCount; setNumber++) {
            workoutExercise.sets.add(WorkoutSet.create(workoutExercise, setNumber));
        }

        return workoutExercise;
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

    public String getExerciseName() {
        return exerciseName;
    }

    public int getPosition() {
        return position;
    }

    public Set<WorkoutSet> getSets() {
        return sets;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    WorkoutSet addSet() {
        int nextSetNumber = sets.stream()
                .mapToInt(WorkoutSet::getSetNumber)
                .max()
                .orElse(0) + 1;
        WorkoutSet workoutSet = WorkoutSet.create(this, nextSetNumber);
        sets.add(workoutSet);
        return workoutSet;
    }

    void removeSet(UUID setId) {
        WorkoutSet set = findSet(setId)
                .orElseThrow(SessionResourceNotFoundException::new);
        sets.remove(set);
    }

    void renumberSets() {
        int setNumber = 1;
        for (WorkoutSet set : sets.stream()
                .sorted(Comparator.comparingInt(WorkoutSet::getSetNumber))
                .toList()) {
            set.setSetNumber(setNumber++);
        }
    }

    Optional<WorkoutSet> findSet(UUID setId) {
        return sets.stream()
                .filter(workoutSet -> workoutSet.getId().equals(setId))
                .findFirst();
    }
}
