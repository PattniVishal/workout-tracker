package com.workouttracker.exercise.api;

import com.workouttracker.common.exception.ApiErrorResponse;
import com.workouttracker.common.openapi.AuthenticatedOperation;
import com.workouttracker.exercise.application.ExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Exercises", description = "Exercise library and custom exercise management")
@AuthenticatedOperation
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping
    @Operation(summary = "List pickable exercises", description = "Returns system exercises and the user's non-archived custom exercises.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exercise list"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid query parameters",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ExerciseListResponse listExercises(
            @Parameter(description = "Optional case-insensitive name search") @RequestParam(required = false) String q,
            @Parameter(description = "Optional primary muscle group filter") @RequestParam(required = false) String muscleGroup
    ) {
        return exerciseService.listPickable(q, muscleGroup);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create custom exercise")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Custom exercise created"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ExerciseResponse createExercise(@Valid @RequestBody CreateExerciseRequest request) {
        return exerciseService.createCustom(request);
    }

    @PutMapping("/{exerciseId}")
    @Operation(summary = "Update custom exercise")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Custom exercise updated"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "System exercise cannot be modified",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Exercise not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ExerciseResponse updateExercise(
            @Parameter(description = "Custom exercise ID") @PathVariable UUID exerciseId,
            @Valid @RequestBody CreateExerciseRequest request
    ) {
        return exerciseService.updateCustom(exerciseId, request);
    }

    @PostMapping("/{exerciseId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archive custom exercise", description = "Archives a custom exercise owned by the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Exercise archived"),
            @ApiResponse(
                    responseCode = "403",
                    description = "System exercise cannot be modified",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Exercise not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public void archiveExercise(@Parameter(description = "Custom exercise ID") @PathVariable UUID exerciseId) {
        exerciseService.archiveCustom(exerciseId);
    }
}
