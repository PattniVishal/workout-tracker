package com.workouttracker.exercise.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "exercise")
public class Exercise {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "primary_muscle_group", nullable = false, length = 50)
    private String primaryMuscleGroup;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "secondary_muscle_groups", nullable = false)
    private List<String> secondaryMuscleGroups = new ArrayList<>();

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Exercise() {
    }

    public static Exercise createCustom(
            String name,
            String primaryMuscleGroup,
            List<String> secondaryMuscleGroups,
            String category,
            UUID ownerId
    ) {
        Exercise exercise = new Exercise();
        exercise.name = name;
        exercise.primaryMuscleGroup = primaryMuscleGroup;
        exercise.secondaryMuscleGroups = copySecondaryMuscleGroups(secondaryMuscleGroups);
        exercise.category = category;
        exercise.createdByUserId = ownerId;
        return exercise;
    }

    public boolean isSystem() {
        return createdByUserId == null;
    }

    public boolean isCustom() {
        return createdByUserId != null;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public boolean isOwnedBy(UUID userId) {
        return createdByUserId != null && createdByUserId.equals(userId);
    }

    public void updateCatalogFields(
            String name,
            String primaryMuscleGroup,
            List<String> secondaryMuscleGroups,
            String category
    ) {
        this.name = name;
        this.primaryMuscleGroup = primaryMuscleGroup;
        this.secondaryMuscleGroups = copySecondaryMuscleGroups(secondaryMuscleGroups);
        this.category = category;
    }

    public void archive(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    private static List<String> copySecondaryMuscleGroups(List<String> secondaryMuscleGroups) {
        if (secondaryMuscleGroups == null || secondaryMuscleGroups.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(secondaryMuscleGroups);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (secondaryMuscleGroups == null) {
            secondaryMuscleGroups = List.of();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPrimaryMuscleGroup() {
        return primaryMuscleGroup;
    }

    public List<String> getSecondaryMuscleGroups() {
        return secondaryMuscleGroups;
    }

    public String getCategory() {
        return category;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "Exercise{id=" + id + ", name='" + name + "'}";
    }
}
