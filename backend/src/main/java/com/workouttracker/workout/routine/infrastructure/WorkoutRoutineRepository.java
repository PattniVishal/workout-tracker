package com.workouttracker.workout.routine.infrastructure;

import com.workouttracker.workout.routine.domain.WorkoutRoutine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutRoutineRepository extends JpaRepository<WorkoutRoutine, UUID> {

    List<WorkoutRoutine> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    @Query("""
            SELECT DISTINCT routine FROM WorkoutRoutine routine
            LEFT JOIN FETCH routine.exercises
            WHERE routine.id = :id AND routine.userId = :userId
            """)
    Optional<WorkoutRoutine> findDetailedByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
