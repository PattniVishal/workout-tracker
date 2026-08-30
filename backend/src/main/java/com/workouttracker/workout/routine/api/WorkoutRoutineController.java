package com.workouttracker.workout.routine.api;

import com.workouttracker.common.exception.ApiErrorResponse;
import com.workouttracker.common.openapi.AuthenticatedOperation;
import com.workouttracker.workout.routine.application.WorkoutRoutineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Routines", description = "Workout routine management")
@AuthenticatedOperation
public class WorkoutRoutineController {

    private final WorkoutRoutineService workoutRoutineService;

    public WorkoutRoutineController(WorkoutRoutineService workoutRoutineService) {
        this.workoutRoutineService = workoutRoutineService;
    }

    @GetMapping
    @Operation(summary = "List routines")
    @ApiResponse(responseCode = "200", description = "Routine summaries for the current user")
    public RoutineListResponse listRoutines() {
        return workoutRoutineService.listByUser();
    }

    @GetMapping("/{routineId}")
    @Operation(summary = "Get routine details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Routine detail"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Routine not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public RoutineDetailResponse getRoutine(@Parameter(description = "Routine ID") @PathVariable UUID routineId) {
        return workoutRoutineService.getById(routineId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create routine")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Routine created"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error or exercise not pickable (`EXERCISE_NOT_PICKABLE`)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Duplicate exercise in routine (`DUPLICATE_ROUTINE_EXERCISE`)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public RoutineDetailResponse createRoutine(@Valid @RequestBody SaveRoutineRequest request) {
        return workoutRoutineService.create(request);
    }

    @PutMapping("/{routineId}")
    @Operation(summary = "Update routine")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Routine updated"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error or exercise not pickable (`EXERCISE_NOT_PICKABLE`)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Routine not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Duplicate exercise in routine (`DUPLICATE_ROUTINE_EXERCISE`)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public RoutineDetailResponse updateRoutine(
            @Parameter(description = "Routine ID") @PathVariable UUID routineId,
            @Valid @RequestBody SaveRoutineRequest request
    ) {
        return workoutRoutineService.update(routineId, request);
    }

    @DeleteMapping("/{routineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete routine")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Routine deleted"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Routine not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public void deleteRoutine(@Parameter(description = "Routine ID") @PathVariable UUID routineId) {
        workoutRoutineService.delete(routineId);
    }
}
