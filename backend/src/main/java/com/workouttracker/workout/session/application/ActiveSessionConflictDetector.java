package com.workouttracker.workout.session.application;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

/**
 * Detects violations of the partial unique index that enforces one IN_PROGRESS session per user.
 */
public final class ActiveSessionConflictDetector {

    public static final String CONSTRAINT_NAME = "uk_workout_session_user_in_progress";

    private ActiveSessionConflictDetector() {
    }

    public static boolean isActiveSessionConflict(DataIntegrityViolationException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation) {
                if (matchesConstraintName(constraintViolation.getConstraintName())) {
                    return true;
                }
            }

            if (current instanceof SQLException sqlException) {
                if (isUniqueViolation(sqlException) && messageReferencesActiveSessionConstraint(sqlException)) {
                    return true;
                }
            }

            current = current.getCause();
        }

        return false;
    }

    private static boolean matchesConstraintName(String constraintName) {
        return constraintName != null && CONSTRAINT_NAME.equalsIgnoreCase(constraintName);
    }

    private static boolean isUniqueViolation(SQLException sqlException) {
        return "23505".equals(sqlException.getSQLState());
    }

    private static boolean messageReferencesActiveSessionConstraint(SQLException sqlException) {
        String message = sqlException.getMessage();
        return message != null && message.toLowerCase().contains(CONSTRAINT_NAME.toLowerCase());
    }
}
