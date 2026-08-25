package com.workouttracker.exercise.infrastructure;

import com.workouttracker.exercise.domain.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    @Query("""
            SELECT e FROM Exercise e
            WHERE (e.createdByUserId IS NULL OR (e.createdByUserId = :userId AND e.archivedAt IS NULL))
              AND (:q = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:muscleGroup = '' OR e.primaryMuscleGroup = :muscleGroup)
            ORDER BY e.name ASC
            """)
    List<Exercise> findPickable(
            @Param("userId") UUID userId,
            @Param("q") String q,
            @Param("muscleGroup") String muscleGroup
    );
}
