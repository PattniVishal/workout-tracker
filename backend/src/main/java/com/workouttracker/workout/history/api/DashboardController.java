package com.workouttracker.workout.history.api;

import com.workouttracker.common.openapi.AuthenticatedOperation;
import com.workouttracker.workout.history.application.WorkoutDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard summary for the authenticated user")
@AuthenticatedOperation
public class DashboardController {

    private final WorkoutDashboardService workoutDashboardService;

    public DashboardController(WorkoutDashboardService workoutDashboardService) {
        this.workoutDashboardService = workoutDashboardService;
    }

    @GetMapping
    @Operation(summary = "Get dashboard summary")
    @ApiResponse(responseCode = "200", description = "Dashboard data")
    public DashboardResponse getDashboard() {
        return workoutDashboardService.getDashboard();
    }
}
