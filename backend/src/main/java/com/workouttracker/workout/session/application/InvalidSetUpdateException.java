package com.workouttracker.workout.session.application;

public class InvalidSetUpdateException extends RuntimeException {

    public InvalidSetUpdateException(String message) {
        super(message);
    }
}
