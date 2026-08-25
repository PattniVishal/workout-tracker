package com.workouttracker.workout.history.api;

import com.workouttracker.workout.history.application.WorkoutHistoryService;
import com.workouttracker.workout.session.api.SessionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class WorkoutHistoryController {

    private final WorkoutHistoryService workoutHistoryService;

    public WorkoutHistoryController(WorkoutHistoryService workoutHistoryService) {
        this.workoutHistoryService = workoutHistoryService;
    }

    @GetMapping("/api/history")
    public HistoryListResponse listHistory() {
        return workoutHistoryService.listCompleted();
    }

    @GetMapping("/api/history/{sessionId}")
    public SessionResponse getHistoryDetail(@PathVariable UUID sessionId) {
        return workoutHistoryService.getCompleted(sessionId);
    }

    @DeleteMapping("/api/history/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHistory(@PathVariable UUID sessionId) {
        workoutHistoryService.deleteCompleted(sessionId);
    }

    @GetMapping("/api/exercises/{exerciseId}/previous-performance")
    public ResponseEntity<PreviousPerformanceResponse> getPreviousPerformance(@PathVariable UUID exerciseId) {
        return workoutHistoryService.getPreviousPerformance(exerciseId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
