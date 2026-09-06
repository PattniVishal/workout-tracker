package com.workouttracker.workout.session.application;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveSessionConflictDetectorTest {

    @Test
    void recognizesHibernateConstraintViolationByConstraintName() {
        SQLException sqlException = new SQLException(
                "duplicate key value violates unique constraint \"uk_workout_session_user_in_progress\"",
                "23505"
        );
        ConstraintViolationException hibernateException = new ConstraintViolationException(
                "could not execute statement",
                sqlException,
                ActiveSessionConflictDetector.CONSTRAINT_NAME
        );
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement",
                hibernateException
        );

        assertThat(ActiveSessionConflictDetector.isActiveSessionConflict(exception)).isTrue();
    }

    @Test
    void recognizesPostgresUniqueViolationFromSqlExceptionMessage() {
        SQLException sqlException = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"uk_workout_session_user_in_progress\"",
                "23505"
        );
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement",
                sqlException
        );

        assertThat(ActiveSessionConflictDetector.isActiveSessionConflict(exception)).isTrue();
    }

    @Test
    void doesNotMapUnrelatedUniqueConstraintViolations() {
        SQLException sqlException = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"uk_app_user_email\"",
                "23505"
        );
        ConstraintViolationException hibernateException = new ConstraintViolationException(
                "could not execute statement",
                sqlException,
                "uk_app_user_email"
        );
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement",
                hibernateException
        );

        assertThat(ActiveSessionConflictDetector.isActiveSessionConflict(exception)).isFalse();
    }

    @Test
    void doesNotMapUnrelatedDataIntegrityViolations() {
        SQLException sqlException = new SQLException(
                "ERROR: null value in column \"name\" violates not-null constraint",
                "23502"
        );
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement",
                sqlException
        );

        assertThat(ActiveSessionConflictDetector.isActiveSessionConflict(exception)).isFalse();
    }
}
