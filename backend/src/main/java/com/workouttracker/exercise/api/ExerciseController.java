package com.workouttracker.exercise.api;

import com.workouttracker.exercise.application.ExerciseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public ExerciseListResponse listExercises(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String muscleGroup
    ) {
        return exerciseService.listPickable(q, muscleGroup);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseResponse createExercise(@Valid @RequestBody CreateExerciseRequest request) {
        return exerciseService.createCustom(request);
    }

    @PutMapping("/{exerciseId}")
    public ExerciseResponse updateExercise(
            @PathVariable UUID exerciseId,
            @Valid @RequestBody CreateExerciseRequest request
    ) {
        return exerciseService.updateCustom(exerciseId, request);
    }

    @PostMapping("/{exerciseId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveExercise(@PathVariable UUID exerciseId) {
        exerciseService.archiveCustom(exerciseId);
    }
}
