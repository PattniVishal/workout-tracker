package com.workouttracker.workout.session.api;

import com.workouttracker.common.exception.ApiErrorResponse;
import com.workouttracker.common.openapi.AuthenticatedOperation;
import com.workouttracker.workout.session.application.WorkoutSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Workout Sessions", description = "Active workout session lifecycle and set logging")
@AuthenticatedOperation
public class WorkoutSessionController {

    private final WorkoutSessionService workoutSessionService;

    public WorkoutSessionController(WorkoutSessionService workoutSessionService) {
        this.workoutSessionService = workoutSessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Start workout session",
            description = "Starts a session from a routine (`routineId`) or as an ad-hoc workout (`name`). Exactly one must be provided."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Session started"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid start request (`INVALID_REQUEST`)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Routine not found when starting from routine",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Active session already exists (`ACTIVE_SESSION_EXISTS`)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public SessionResponse startSession(@RequestBody StartSessionRequest request) {
        return workoutSessionService.start(request);
    }

    @GetMapping("/current")
    @Operation(summary = "Get current in-progress session")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current in-progress session"),
            @ApiResponse(responseCode = "204", description = "No in-progress session")
    })
    public ResponseEntity<SessionResponse> getCurrentSession() {
        return workoutSessionService.getInProgress()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/current/exercises")
    @Operation(summary = "Add exercise to current session")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated session"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error or exercise not pickable (`EXERCISE_NOT_PICKABLE`)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No in-progress session (`NOT_FOUND`)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public SessionResponse addExercise(@Valid @RequestBody AddSessionExerciseRequest request) {
        return workoutSessionService.addExercise(request);
    }

    @DeleteMapping("/current/exercises/{workoutExerciseId}")
    @Operation(summary = "Remove exercise from current session")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated session"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Session or workout exercise not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public SessionResponse removeExercise(
            @Parameter(description = "Workout exercise ID within the session") @PathVariable UUID workoutExerciseId
    ) {
        return workoutSessionService.removeExercise(workoutExerciseId);
    }

    @PostMapping("/current/exercises/{workoutExerciseId}/sets")
    @Operation(summary = "Add set to workout exercise")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated session"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Session or workout exercise not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public SessionResponse addSet(
            @Parameter(description = "Workout exercise ID within the session") @PathVariable UUID workoutExerciseId
    ) {
        return workoutSessionService.addSet(workoutExerciseId);
    }

    @PatchMapping("/current/exercises/{workoutExerciseId}/sets/{setId}")
    @Operation(summary = "Update workout set")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated session"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid set update (`INVALID_REQUEST`)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Session, exercise, or set not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public SessionResponse updateSet(
            @Parameter(description = "Workout exercise ID within the session") @PathVariable UUID workoutExerciseId,
            @Parameter(description = "Set ID") @PathVariable UUID setId,
            @Valid @RequestBody UpdateSetRequest request
    ) {
        return workoutSessionService.updateSet(workoutExerciseId, setId, request);
    }

    @DeleteMapping("/current/exercises/{workoutExerciseId}/sets/{setId}")
    @Operation(summary = "Delete workout set")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated session"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Session, exercise, or set not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public SessionResponse removeSet(
            @Parameter(description = "Workout exercise ID within the session") @PathVariable UUID workoutExerciseId,
            @Parameter(description = "Set ID") @PathVariable UUID setId
    ) {
        return workoutSessionService.removeSet(workoutExerciseId, setId);
    }

    @PostMapping("/current/complete")
    @Operation(summary = "Complete current workout")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Completed session"),
            @ApiResponse(
                    responseCode = "404",
                    description = "No in-progress session",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "No completed sets (`NO_COMPLETED_SETS`) or session not in progress (`SESSION_NOT_IN_PROGRESS`)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public SessionResponse completeCurrentSession() {
        return workoutSessionService.complete();
    }

    @DeleteMapping("/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Discard current workout", description = "Permanently deletes the in-progress session and all logged sets.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Session discarded"),
            @ApiResponse(
                    responseCode = "404",
                    description = "No in-progress session",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public void discardCurrentSession() {
        workoutSessionService.discard();
    }
}
