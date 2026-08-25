package com.workouttracker.workout.session.api;

import com.workouttracker.workout.session.application.WorkoutSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class WorkoutSessionController {

    private final WorkoutSessionService workoutSessionService;

    public WorkoutSessionController(WorkoutSessionService workoutSessionService) {
        this.workoutSessionService = workoutSessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse startSession(@RequestBody StartSessionRequest request) {
        return workoutSessionService.start(request);
    }

    @GetMapping("/current")
    public ResponseEntity<SessionResponse> getCurrentSession() {
        return workoutSessionService.getInProgress()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/current/exercises")
    public SessionResponse addExercise(@Valid @RequestBody AddSessionExerciseRequest request) {
        return workoutSessionService.addExercise(request);
    }

    @DeleteMapping("/current/exercises/{workoutExerciseId}")
    public SessionResponse removeExercise(@PathVariable UUID workoutExerciseId) {
        return workoutSessionService.removeExercise(workoutExerciseId);
    }

    @PostMapping("/current/exercises/{workoutExerciseId}/sets")
    public SessionResponse addSet(@PathVariable UUID workoutExerciseId) {
        return workoutSessionService.addSet(workoutExerciseId);
    }

    @PatchMapping("/current/exercises/{workoutExerciseId}/sets/{setId}")
    public SessionResponse updateSet(
            @PathVariable UUID workoutExerciseId,
            @PathVariable UUID setId,
            @Valid @RequestBody UpdateSetRequest request
    ) {
        return workoutSessionService.updateSet(workoutExerciseId, setId, request);
    }

    @DeleteMapping("/current/exercises/{workoutExerciseId}/sets/{setId}")
    public SessionResponse removeSet(@PathVariable UUID workoutExerciseId, @PathVariable UUID setId) {
        return workoutSessionService.removeSet(workoutExerciseId, setId);
    }

    @PostMapping("/current/complete")
    public SessionResponse completeCurrentSession() {
        return workoutSessionService.complete();
    }

    @DeleteMapping("/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discardCurrentSession() {
        workoutSessionService.discard();
    }
}
