package com.workouttracker.workout.history.api;

import com.workouttracker.common.exception.ApiErrorResponse;
import com.workouttracker.common.openapi.AuthenticatedOperation;
import com.workouttracker.workout.history.application.WorkoutHistoryService;
import com.workouttracker.workout.session.api.SessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "History", description = "Completed workout history and previous performance")
@AuthenticatedOperation
public class WorkoutHistoryController {

    private final WorkoutHistoryService workoutHistoryService;

    public WorkoutHistoryController(WorkoutHistoryService workoutHistoryService) {
        this.workoutHistoryService = workoutHistoryService;
    }

    @GetMapping("/api/history")
    @Operation(summary = "List completed workouts")
    @ApiResponse(responseCode = "200", description = "Completed workout summaries")
    public HistoryListResponse listHistory() {
        return workoutHistoryService.listCompleted();
    }

    @GetMapping("/api/history/{sessionId}")
    @Operation(summary = "Get completed workout detail")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Completed session detail"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Workout history not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public SessionResponse getHistoryDetail(
            @Parameter(description = "Completed session ID") @PathVariable UUID sessionId
    ) {
        return workoutHistoryService.getCompleted(sessionId);
    }

    @DeleteMapping("/api/history/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete completed workout")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "History entry deleted"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Workout history not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public void deleteHistory(@Parameter(description = "Completed session ID") @PathVariable UUID sessionId) {
        workoutHistoryService.deleteCompleted(sessionId);
    }

    @GetMapping("/api/exercises/{exerciseId}/previous-performance")
    @Operation(
            summary = "Get previous performance for an exercise",
            description = "Returns the most recent completed workout performance for the exercise, if any."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Previous performance found"),
            @ApiResponse(responseCode = "204", description = "No previous performance available")
    })
    public ResponseEntity<PreviousPerformanceResponse> getPreviousPerformance(
            @Parameter(description = "Exercise ID") @PathVariable UUID exerciseId
    ) {
        return workoutHistoryService.getPreviousPerformance(exerciseId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
