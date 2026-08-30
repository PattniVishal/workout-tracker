package com.workouttracker.workout.session.application;

import com.workouttracker.common.security.CurrentUser;
import com.workouttracker.exercise.application.ExerciseService;
import com.workouttracker.exercise.domain.Exercise;
import com.workouttracker.workout.routine.application.RoutineNotFoundException;
import com.workouttracker.workout.routine.domain.RoutineExercise;
import com.workouttracker.workout.routine.domain.WorkoutRoutine;
import com.workouttracker.workout.routine.infrastructure.WorkoutRoutineRepository;
import com.workouttracker.workout.session.api.AddSessionExerciseRequest;
import com.workouttracker.workout.session.api.SessionResponse;
import com.workouttracker.workout.session.api.StartSessionRequest;
import com.workouttracker.workout.session.api.UpdateSetRequest;
import com.workouttracker.workout.session.domain.WorkoutSession;
import com.workouttracker.workout.session.domain.WorkoutSessionStatus;
import com.workouttracker.workout.session.infrastructure.WorkoutSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkoutSessionService {

    private static final Logger log = LoggerFactory.getLogger(WorkoutSessionService.class);

    private final WorkoutSessionRepository workoutSessionRepository;
    private final WorkoutRoutineRepository workoutRoutineRepository;
    private final ExerciseService exerciseService;
    private final CurrentUser currentUser;
    private final SessionResponseMapper sessionResponseMapper;

    public WorkoutSessionService(
            WorkoutSessionRepository workoutSessionRepository,
            WorkoutRoutineRepository workoutRoutineRepository,
            ExerciseService exerciseService,
            CurrentUser currentUser,
            SessionResponseMapper sessionResponseMapper
    ) {
        this.workoutSessionRepository = workoutSessionRepository;
        this.workoutRoutineRepository = workoutRoutineRepository;
        this.exerciseService = exerciseService;
        this.currentUser = currentUser;
        this.sessionResponseMapper = sessionResponseMapper;
    }

    @Transactional
    public SessionResponse start(StartSessionRequest request) {
        UUID userId = currentUser.requireUserId();
        assertNoActiveSession(userId);

        boolean hasRoutine = request.routineId() != null;
        boolean hasName = request.name() != null && !request.name().isBlank();
        if (hasRoutine == hasName) {
            throw new InvalidSessionStartRequestException();
        }

        SessionResponse response;
        if (hasRoutine) {
            response = startFromRoutine(userId, request.routineId());
        } else {
            response = startEmpty(userId, request.name().trim());
        }

        log.info("Workout session started userId={} sessionId={}", userId, response.id());
        return response;
    }

    @Transactional(readOnly = true)
    public Optional<SessionResponse> getInProgress() {
        UUID userId = currentUser.requireUserId();
        return workoutSessionRepository
                .findDetailedByUserIdAndStatus(userId, WorkoutSessionStatus.IN_PROGRESS)
                .map(this::toResponse);
    }

    @Transactional
    public SessionResponse addExercise(AddSessionExerciseRequest request) {
        UUID userId = currentUser.requireUserId();
        WorkoutSession session = requireInProgressSession(userId);
        Exercise exercise = exerciseService.requirePickable(request.exerciseId(), userId);
        session.addExercise(exercise.getId(), exercise.getName(), request.initialSetCount());
        WorkoutSession saved = workoutSessionRepository.saveAndFlush(session);
        log.info(
                "Workout exercise added userId={} sessionId={} exerciseId={}",
                userId,
                saved.getId(),
                request.exerciseId()
        );
        return toResponse(saved);
    }

    @Transactional
    public SessionResponse removeExercise(UUID workoutExerciseId) {
        UUID userId = currentUser.requireUserId();
        WorkoutSession session = requireInProgressSession(userId);
        session.removeExercise(workoutExerciseId);
        WorkoutSession saved = workoutSessionRepository.saveAndFlush(session);
        log.info(
                "Workout exercise removed userId={} sessionId={} workoutExerciseId={}",
                userId,
                saved.getId(),
                workoutExerciseId
        );
        return toResponse(saved);
    }

    @Transactional
    public SessionResponse addSet(UUID workoutExerciseId) {
        UUID userId = currentUser.requireUserId();
        WorkoutSession session = requireInProgressSession(userId);
        session.addSet(workoutExerciseId);
        WorkoutSession saved = workoutSessionRepository.saveAndFlush(session);
        log.info(
                "Workout set added userId={} sessionId={} workoutExerciseId={}",
                userId,
                saved.getId(),
                workoutExerciseId
        );
        return toResponse(saved);
    }

    @Transactional
    public SessionResponse updateSet(UUID workoutExerciseId, UUID setId, UpdateSetRequest request) {
        UUID userId = currentUser.requireUserId();
        WorkoutSession session = requireInProgressSession(userId);
        session.updateSet(workoutExerciseId, setId, request.weightKg(), request.repetitions(), request.completed());
        WorkoutSession saved = workoutSessionRepository.saveAndFlush(session);
        log.info(
                "Workout set updated userId={} sessionId={} workoutExerciseId={} setId={}",
                userId,
                saved.getId(),
                workoutExerciseId,
                setId
        );
        return toResponse(saved);
    }

    @Transactional
    public SessionResponse removeSet(UUID workoutExerciseId, UUID setId) {
        UUID userId = currentUser.requireUserId();
        WorkoutSession session = requireInProgressSession(userId);
        session.removeSet(workoutExerciseId, setId);
        workoutSessionRepository.saveAndFlush(session);
        session.renumberSets(workoutExerciseId);
        WorkoutSession saved = workoutSessionRepository.saveAndFlush(session);
        log.info(
                "Workout set removed userId={} sessionId={} workoutExerciseId={} setId={}",
                userId,
                saved.getId(),
                workoutExerciseId,
                setId
        );
        return toResponse(saved);
    }

    @Transactional
    public SessionResponse complete() {
        UUID userId = currentUser.requireUserId();
        WorkoutSession session = requireInProgressSession(userId);
        session.complete(Instant.now());
        WorkoutSession saved = workoutSessionRepository.saveAndFlush(session);
        log.info("Workout session completed userId={} sessionId={}", userId, saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void discard() {
        UUID userId = currentUser.requireUserId();
        WorkoutSession session = requireInProgressSession(userId);
        UUID sessionId = session.getId();
        workoutSessionRepository.delete(session);
        workoutSessionRepository.flush();
        log.info("Workout session discarded userId={} sessionId={}", userId, sessionId);
    }

    private WorkoutSession requireInProgressSession(UUID userId) {
        return workoutSessionRepository
                .findDetailedByUserIdAndStatus(userId, WorkoutSessionStatus.IN_PROGRESS)
                .orElseThrow(NoCurrentSessionException::new);
    }

    private SessionResponse startFromRoutine(UUID userId, UUID routineId) {
        WorkoutRoutine routine = workoutRoutineRepository.findDetailedByIdAndUserId(routineId, userId)
                .orElseThrow(RoutineNotFoundException::new);

        Map<UUID, String> exerciseNamesById = new HashMap<>();
        for (RoutineExercise slot : routine.getExercises()) {
            Exercise exercise = exerciseService.requireResolvableReference(slot.getExerciseId(), userId);
            exerciseNamesById.put(exercise.getId(), exercise.getName());
        }

        WorkoutSession session = WorkoutSession.startFromRoutine(userId, routine, exerciseNamesById);
        return toResponse(saveSession(session));
    }

    private SessionResponse startEmpty(UUID userId, String name) {
        WorkoutSession session = WorkoutSession.startEmpty(userId, name);
        return toResponse(saveSession(session));
    }

    private WorkoutSession saveSession(WorkoutSession session) {
        try {
            return workoutSessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException ex) {
            if (ActiveSessionConflictDetector.isActiveSessionConflict(ex)) {
                log.warn("Active workout session conflict userId={}", session.getUserId());
                throw new ActiveSessionExistsException();
            }
            throw ex;
        }
    }

    private void assertNoActiveSession(UUID userId) {
        if (workoutSessionRepository.existsByUserIdAndStatus(userId, WorkoutSessionStatus.IN_PROGRESS)) {
            log.warn("Active workout session conflict userId={}", userId);
            throw new ActiveSessionExistsException();
        }
    }

    private SessionResponse toResponse(WorkoutSession session) {
        return sessionResponseMapper.toResponse(session);
    }
}
