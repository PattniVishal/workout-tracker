package com.workouttracker.workout.history.api;

import com.workouttracker.workout.history.application.WorkoutDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final WorkoutDashboardService workoutDashboardService;

    public DashboardController(WorkoutDashboardService workoutDashboardService) {
        this.workoutDashboardService = workoutDashboardService;
    }

    @GetMapping
    public DashboardResponse getDashboard() {
        return workoutDashboardService.getDashboard();
    }
}
