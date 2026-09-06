package com.workouttracker.workout.routine.application;

import com.workouttracker.common.security.CurrentUser;
import com.workouttracker.exercise.application.ExerciseService;
import com.workouttracker.exercise.domain.Exercise;
import com.workouttracker.workout.routine.api.RoutineDetailResponse;
import com.workouttracker.workout.routine.api.RoutineExerciseResponse;
import com.workouttracker.workout.routine.api.RoutineListResponse;
import com.workouttracker.workout.routine.api.RoutineSummaryResponse;
import com.workouttracker.workout.routine.api.SaveRoutineRequest;
import com.workouttracker.workout.routine.domain.RoutineExercise;
import com.workouttracker.workout.routine.domain.RoutineExerciseSlot;
import com.workouttracker.workout.routine.domain.WorkoutRoutine;
import com.workouttracker.workout.routine.infrastructure.WorkoutRoutineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkoutRoutineService {

    private static final Logger log = LoggerFactory.getLogger(WorkoutRoutineService.class);

    private final WorkoutRoutineRepository workoutRoutineRepository;
    private final ExerciseService exerciseService;
    private final CurrentUser currentUser;

    public WorkoutRoutineService(
            WorkoutRoutineRepository workoutRoutineRepository,
            ExerciseService exerciseService,
            CurrentUser currentUser
    ) {
        this.workoutRoutineRepository = workoutRoutineRepository;
        this.exerciseService = exerciseService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public RoutineListResponse listByUser() {
        UUID userId = currentUser.requireUserId();

        List<RoutineSummaryResponse> routines = workoutRoutineRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();

        return new RoutineListResponse(routines);
    }

    @Transactional(readOnly = true)
    public RoutineDetailResponse getById(UUID routineId) {
        UUID userId = currentUser.requireUserId();
        WorkoutRoutine routine = requireOwnedRoutine(routineId, userId);
        return toDetailResponse(routine, resolveExerciseNames(routine, userId));
    }

    @Transactional
    public RoutineDetailResponse create(SaveRoutineRequest request) {
        UUID userId = currentUser.requireUserId();
        List<ResolvedRoutineExerciseSlot> slots = resolveExerciseSlots(request, userId, Set.of());

        WorkoutRoutine routine = WorkoutRoutine.create(userId, request.name(), request.description());
        routine.addExerciseSlots(toDomainSlots(slots));

        WorkoutRoutine saved = workoutRoutineRepository.save(routine);
        log.info("Routine created userId={} routineId={}", userId, saved.getId());
        return toDetailResponse(saved, toExerciseNameMap(slots));
    }

    @Transactional
    public RoutineDetailResponse update(UUID routineId, SaveRoutineRequest request) {
        UUID userId = currentUser.requireUserId();
        WorkoutRoutine routine = requireOwnedRoutine(routineId, userId);
        Set<UUID> retainedExerciseIds = routine.getExercises().stream()
                .map(RoutineExercise::getExerciseId)
                .collect(Collectors.toSet());
        List<ResolvedRoutineExerciseSlot> slots = resolveExerciseSlots(request, userId, retainedExerciseIds);

        routine.updateDetails(request.name(), request.description());
        routine.clearExercises();
        workoutRoutineRepository.flush();
        routine.addExerciseSlots(toDomainSlots(slots));

        log.info("Routine updated userId={} routineId={}", userId, routineId);
        return toDetailResponse(routine, toExerciseNameMap(slots));
    }

    @Transactional
    public void delete(UUID routineId) {
        UUID userId = currentUser.requireUserId();
        if (!workoutRoutineRepository.existsByIdAndUserId(routineId, userId)) {
            throw new RoutineNotFoundException();
        }
        workoutRoutineRepository.deleteByIdAndUserId(routineId, userId);
        log.info("Routine deleted userId={} routineId={}", userId, routineId);
    }

    private WorkoutRoutine requireOwnedRoutine(UUID routineId, UUID userId) {
        return workoutRoutineRepository.findDetailedByIdAndUserId(routineId, userId)
                .orElseThrow(RoutineNotFoundException::new);
    }

    private List<ResolvedRoutineExerciseSlot> resolveExerciseSlots(
            SaveRoutineRequest request,
            UUID userId,
            Set<UUID> retainedExerciseIds
    ) {
        assertNoDuplicateExerciseIds(request);

        List<ResolvedRoutineExerciseSlot> slots = new ArrayList<>();
        for (int index = 0; index < request.exercises().size(); index++) {
            SaveRoutineRequest.RoutineExerciseRequest slotRequest = request.exercises().get(index);
            boolean retainedReference = retainedExerciseIds.contains(slotRequest.exerciseId());
            Exercise exercise = resolveExerciseForRoutineSlot(slotRequest.exerciseId(), userId, retainedReference);
            slots.add(new ResolvedRoutineExerciseSlot(
                    exercise.getId(),
                    exercise.getName(),
                    index + 1,
                    slotRequest.plannedSetCount()
            ));
        }
        return slots;
    }

    private Exercise resolveExerciseForRoutineSlot(UUID exerciseId, UUID userId, boolean retainedReference) {
        if (retainedReference) {
            return exerciseService.requireResolvableReference(exerciseId, userId);
        }
        return exerciseService.requirePickable(exerciseId, userId);
    }

    private void assertNoDuplicateExerciseIds(SaveRoutineRequest request) {
        Set<UUID> seenExerciseIds = new HashSet<>();
        for (SaveRoutineRequest.RoutineExerciseRequest slot : request.exercises()) {
            if (!seenExerciseIds.add(slot.exerciseId())) {
                throw new DuplicateRoutineExerciseException();
            }
        }
    }

    private List<RoutineExerciseSlot> toDomainSlots(List<ResolvedRoutineExerciseSlot> slots) {
        return slots.stream()
                .map(slot -> new RoutineExerciseSlot(slot.exerciseId(), slot.position(), slot.plannedSetCount()))
                .toList();
    }

    private Map<UUID, String> resolveExerciseNames(WorkoutRoutine routine, UUID userId) {
        return routine.getExercises().stream()
                .map(RoutineExercise::getExerciseId)
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        exerciseId -> exerciseService.requireResolvableReference(exerciseId, userId).getName()
                ));
    }

    private Map<UUID, String> toExerciseNameMap(List<ResolvedRoutineExerciseSlot> slots) {
        return slots.stream()
                .collect(Collectors.toMap(
                        ResolvedRoutineExerciseSlot::exerciseId,
                        ResolvedRoutineExerciseSlot::exerciseName,
                        (left, right) -> left
                ));
    }

    private RoutineSummaryResponse toSummaryResponse(WorkoutRoutine routine) {
        return new RoutineSummaryResponse(
                routine.getId(),
                routine.getName(),
                routine.getDescription(),
                routine.getExercises().size(),
                routine.getUpdatedAt()
        );
    }

    private RoutineDetailResponse toDetailResponse(WorkoutRoutine routine, Map<UUID, String> exerciseNames) {
        List<RoutineExerciseResponse> exercises = routine.getExercises().stream()
                .sorted(Comparator.comparingInt(RoutineExercise::getPosition))
                .map(slot -> new RoutineExerciseResponse(
                        slot.getId(),
                        slot.getExerciseId(),
                        exerciseNames.get(slot.getExerciseId()),
                        slot.getPlannedSetCount(),
                        slot.getPosition()
                ))
                .toList();

        return new RoutineDetailResponse(
                routine.getId(),
                routine.getName(),
                routine.getDescription(),
                exercises
        );
    }

    private record ResolvedRoutineExerciseSlot(
            UUID exerciseId,
            String exerciseName,
            int position,
            int plannedSetCount
    ) {
    }
}
