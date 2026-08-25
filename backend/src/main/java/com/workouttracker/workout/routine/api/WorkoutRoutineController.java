package com.workouttracker.workout.routine.api;

import com.workouttracker.workout.routine.application.WorkoutRoutineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/routines")
public class WorkoutRoutineController {

    private final WorkoutRoutineService workoutRoutineService;

    public WorkoutRoutineController(WorkoutRoutineService workoutRoutineService) {
        this.workoutRoutineService = workoutRoutineService;
    }

    @GetMapping
    public RoutineListResponse listRoutines() {
        return workoutRoutineService.listByUser();
    }

    @GetMapping("/{routineId}")
    public RoutineDetailResponse getRoutine(@PathVariable UUID routineId) {
        return workoutRoutineService.getById(routineId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoutineDetailResponse createRoutine(@Valid @RequestBody SaveRoutineRequest request) {
        return workoutRoutineService.create(request);
    }

    @PutMapping("/{routineId}")
    public RoutineDetailResponse updateRoutine(
            @PathVariable UUID routineId,
            @Valid @RequestBody SaveRoutineRequest request
    ) {
        return workoutRoutineService.update(routineId, request);
    }

    @DeleteMapping("/{routineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoutine(@PathVariable UUID routineId) {
        workoutRoutineService.delete(routineId);
    }
}
