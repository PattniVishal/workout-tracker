package com.workouttracker.workout.session.infrastructure;

import com.workouttracker.workout.session.domain.WorkoutSession;
import com.workouttracker.workout.session.domain.WorkoutSessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    boolean existsByUserIdAndStatus(UUID userId, WorkoutSessionStatus status);

    long countByUserIdAndStatus(UUID userId, WorkoutSessionStatus status);

    Optional<WorkoutSession> findFirstByUserIdAndStatusOrderByCompletedAtDesc(
            UUID userId,
            WorkoutSessionStatus status
    );

    @Query("""
            SELECT session.id FROM WorkoutSession session
            WHERE session.userId = :userId AND session.status = :status
            """)
    Optional<UUID> findSessionIdByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") WorkoutSessionStatus status
    );

    @Query("""
            SELECT COUNT(workoutSet) FROM WorkoutSet workoutSet
            JOIN workoutSet.workoutExercise workoutExercise
            JOIN workoutExercise.workoutSession session
            WHERE session.userId = :userId
              AND session.status = :status
              AND workoutSet.completed = true
            """)
    long countCompletedSetsByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") WorkoutSessionStatus status
    );

    @Query("""
            SELECT DISTINCT session FROM WorkoutSession session
            LEFT JOIN FETCH session.exercises exercise
            LEFT JOIN FETCH exercise.sets
            WHERE session.userId = :userId AND session.status = :status
            """)
    Optional<WorkoutSession> findDetailedByUserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") WorkoutSessionStatus status
    );

    @Query("""
            SELECT DISTINCT session FROM WorkoutSession session
            LEFT JOIN FETCH session.exercises exercise
            LEFT JOIN FETCH exercise.sets
            WHERE session.userId = :userId AND session.status = :status
            ORDER BY session.completedAt DESC
            """)
    List<WorkoutSession> findCompletedDetailedByUserIdOrderByCompletedAtDesc(
            @Param("userId") UUID userId,
            @Param("status") WorkoutSessionStatus status
    );

    @Query("""
            SELECT DISTINCT session FROM WorkoutSession session
            LEFT JOIN FETCH session.exercises exercise
            LEFT JOIN FETCH exercise.sets
            WHERE session.id = :sessionId AND session.userId = :userId AND session.status = :status
            """)
    Optional<WorkoutSession> findDetailedByIdAndUserIdAndStatus(
            @Param("sessionId") UUID sessionId,
            @Param("userId") UUID userId,
            @Param("status") WorkoutSessionStatus status
    );

    @Query("""
            SELECT DISTINCT session FROM WorkoutSession session
            LEFT JOIN FETCH session.exercises exercise
            LEFT JOIN FETCH exercise.sets
            WHERE session.userId = :userId
              AND session.status = :status
              AND exercise.exerciseId = :exerciseId
            ORDER BY session.completedAt DESC
            """)
    List<WorkoutSession> findCompletedSessionsContainingExerciseOrderByCompletedAtDesc(
            @Param("userId") UUID userId,
            @Param("status") WorkoutSessionStatus status,
            @Param("exerciseId") UUID exerciseId,
            Pageable pageable
    );
}
