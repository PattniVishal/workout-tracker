package com.workouttracker.common.exception;

import java.util.List;

public record ApiErrorResponse(
        int status,
        String code,
        String message,
        List<FieldErrorResponse> fieldErrors
) {

    public static ApiErrorResponse of(int status, String code, String message) {
        return new ApiErrorResponse(status, code, message, List.of());
    }
}
