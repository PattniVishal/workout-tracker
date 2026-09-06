package com.workouttracker.exercise.application;

import com.workouttracker.common.security.CurrentUser;
import com.workouttracker.exercise.api.CreateExerciseRequest;
import com.workouttracker.exercise.api.ExerciseListResponse;
import com.workouttracker.exercise.api.ExerciseResponse;
import com.workouttracker.exercise.domain.Exercise;
import com.workouttracker.exercise.domain.ExerciseSource;
import com.workouttracker.exercise.infrastructure.ExerciseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ExerciseService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseService.class);

    private final ExerciseRepository exerciseRepository;
    private final CurrentUser currentUser;

    public ExerciseService(ExerciseRepository exerciseRepository, CurrentUser currentUser) {
        this.exerciseRepository = exerciseRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public ExerciseListResponse listPickable(String q, String muscleGroup) {
        UUID userId = currentUser.requireUserId();
        String normalizedQuery = normalizeOptionalFilter(q);
        String normalizedMuscleGroup = normalizeOptionalFilter(muscleGroup);

        log.debug(
                "Listing pickable exercises userId={} qPresent={} muscleGroupPresent={}",
                userId,
                !normalizedQuery.isEmpty(),
                !normalizedMuscleGroup.isEmpty()
        );

        List<ExerciseResponse> exercises = exerciseRepository
                .findPickable(userId, normalizedQuery, normalizedMuscleGroup)
                .stream()
                .map(this::toResponse)
                .toList();

        return new ExerciseListResponse(exercises);
    }

    @Transactional
    public ExerciseResponse createCustom(CreateExerciseRequest request) {
        UUID userId = currentUser.requireUserId();
        Exercise exercise = Exercise.createCustom(
                request.name(),
                request.primaryMuscleGroup(),
                request.secondaryMuscleGroups(),
                request.category(),
                userId
        );
        Exercise saved = exerciseRepository.save(exercise);
        log.info("Custom exercise created userId={} exerciseId={}", userId, saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public ExerciseResponse updateCustom(UUID exerciseId, CreateExerciseRequest request) {
        UUID userId = currentUser.requireUserId();
        Exercise exercise = requireOwnedCustom(exerciseId, userId);
        exercise.updateCatalogFields(
                request.name(),
                request.primaryMuscleGroup(),
                request.secondaryMuscleGroups(),
                request.category()
        );
        log.info("Custom exercise updated userId={} exerciseId={}", userId, exerciseId);
        return toResponse(exercise);
    }

    @Transactional
    public void archiveCustom(UUID exerciseId) {
        UUID userId = currentUser.requireUserId();
        Exercise exercise = requireOwnedCustom(exerciseId, userId);
        if (!exercise.isArchived()) {
            exercise.archive(Instant.now());
            log.info("Custom exercise archived userId={} exerciseId={}", userId, exerciseId);
        }
    }

    @Transactional(readOnly = true)
    public Exercise requirePickable(UUID exerciseId, UUID userId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(ExerciseNotPickableException::new);

        boolean pickable = exercise.isSystem()
                || (exercise.isOwnedBy(userId) && !exercise.isArchived());

        if (!pickable) {
            throw new ExerciseNotPickableException();
        }

        return exercise;
    }

    /**
     * Resolves an exercise referenced by an existing routine or session snapshot source.
     * Archived customs remain resolvable for the owner; other users' customs are not accessible.
     */
    @Transactional(readOnly = true)
    public Exercise requireResolvableReference(UUID exerciseId, UUID userId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(ExerciseNotFoundException::new);

        if (exercise.isSystem() || exercise.isOwnedBy(userId)) {
            return exercise;
        }

        throw new ExerciseNotFoundException();
    }

    private Exercise requireOwnedCustom(UUID exerciseId, UUID userId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(ExerciseNotFoundException::new);

        if (exercise.isSystem()) {
            throw new SystemExerciseMutationException();
        }

        if (!exercise.isOwnedBy(userId)) {
            throw new ExerciseNotFoundException();
        }

        return exercise;
    }

    private String normalizeOptionalFilter(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private ExerciseResponse toResponse(Exercise exercise) {
        return new ExerciseResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getPrimaryMuscleGroup(),
                List.copyOf(exercise.getSecondaryMuscleGroups()),
                exercise.getCategory(),
                ExerciseSource.fromCreatedByUserId(exercise.getCreatedByUserId()).name()
        );
    }
}
