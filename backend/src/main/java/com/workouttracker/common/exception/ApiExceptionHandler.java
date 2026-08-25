package com.workouttracker.common.exception;

import com.workouttracker.auth.application.EmailAlreadyRegisteredException;
import com.workouttracker.auth.application.InvalidCredentialsException;
import com.workouttracker.common.security.AuthenticationRequiredException;
import com.workouttracker.exercise.application.ExerciseNotFoundException;
import com.workouttracker.exercise.application.ExerciseNotPickableException;
import com.workouttracker.exercise.application.SystemExerciseMutationException;
import com.workouttracker.workout.history.application.HistorySessionNotFoundException;
import com.workouttracker.workout.routine.application.DuplicateRoutineExerciseException;
import com.workouttracker.workout.routine.application.RoutineNotFoundException;
import com.workouttracker.workout.session.application.ActiveSessionExistsException;
import com.workouttracker.workout.session.application.InvalidSessionStartRequestException;
import com.workouttracker.workout.session.application.InvalidSetUpdateException;
import com.workouttracker.workout.session.application.NoCompletedSetsException;
import com.workouttracker.workout.session.application.NoCurrentSessionException;
import com.workouttracker.workout.session.application.SessionNotInProgressException;
import com.workouttracker.workout.session.application.SessionResourceNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationRequired(AuthenticationRequiredException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "UNAUTHORIZED",
                        "Authentication is required."
                ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "UNAUTHORIZED",
                        "Invalid email or password."
                ));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        "EMAIL_ALREADY_REGISTERED",
                        "Email is already registered."
                ));
    }

    @ExceptionHandler(ExerciseNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleExerciseNotFound(ExerciseNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        "NOT_FOUND",
                        "Exercise not found."
                ));
    }

    @ExceptionHandler(ExerciseNotPickableException.class)
    public ResponseEntity<ApiErrorResponse> handleExerciseNotPickable(ExerciseNotPickableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "EXERCISE_NOT_PICKABLE",
                        "Exercise is not available for selection."
                ));
    }

    @ExceptionHandler(SystemExerciseMutationException.class)
    public ResponseEntity<ApiErrorResponse> handleSystemExerciseMutation(SystemExerciseMutationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        "FORBIDDEN",
                        "System exercises cannot be modified."
                ));
    }

    @ExceptionHandler(RoutineNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRoutineNotFound(RoutineNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        "NOT_FOUND",
                        "Routine not found."
                ));
    }

    @ExceptionHandler(DuplicateRoutineExerciseException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateRoutineExercise(DuplicateRoutineExerciseException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        "DUPLICATE_ROUTINE_EXERCISE",
                        "Routine cannot contain duplicate exercises."
                ));
    }

    @ExceptionHandler(ActiveSessionExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleActiveSessionExists(ActiveSessionExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        "ACTIVE_SESSION_EXISTS",
                        "An in-progress workout already exists."
                ));
    }

    @ExceptionHandler(InvalidSessionStartRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSessionStartRequest(InvalidSessionStartRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "INVALID_REQUEST",
                        "Exactly one of routineId or name must be provided."
                ));
    }

    @ExceptionHandler(NoCurrentSessionException.class)
    public ResponseEntity<ApiErrorResponse> handleNoCurrentSession(NoCurrentSessionException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        "NOT_FOUND",
                        "No in-progress workout session."
                ));
    }

    @ExceptionHandler(HistorySessionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleHistorySessionNotFound(HistorySessionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        "NOT_FOUND",
                        "Workout history not found."
                ));
    }

    @ExceptionHandler(NoCompletedSetsException.class)
    public ResponseEntity<ApiErrorResponse> handleNoCompletedSets(NoCompletedSetsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        "NO_COMPLETED_SETS",
                        "Workout must have at least one completed set."
                ));
    }

    @ExceptionHandler(SessionResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionResourceNotFound(SessionResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        "NOT_FOUND",
                        "Workout resource not found."
                ));
    }

    @ExceptionHandler(SessionNotInProgressException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionNotInProgress(SessionNotInProgressException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        "SESSION_NOT_IN_PROGRESS",
                        "Workout session is not in progress."
                ));
    }

    @ExceptionHandler(InvalidSetUpdateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSetUpdate(InvalidSetUpdateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "INVALID_REQUEST",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<FieldErrorResponse> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "VALIDATION_ERROR",
                        "Request is invalid.",
                        fieldErrors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorResponse> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(this::toFieldError)
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "VALIDATION_ERROR",
                        "Request is invalid.",
                        fieldErrors
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "MALFORMED_REQUEST",
                        "Request body is malformed."
                ));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "INVALID_REQUEST",
                        "Request parameters are invalid."
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "INTERNAL_ERROR",
                        "An unexpected error occurred."
                ));
    }

    private FieldErrorResponse toFieldError(FieldError fieldError) {
        return new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private FieldErrorResponse toFieldError(ConstraintViolation<?> violation) {
        return new FieldErrorResponse(violation.getPropertyPath().toString(), violation.getMessage());
    }
}
