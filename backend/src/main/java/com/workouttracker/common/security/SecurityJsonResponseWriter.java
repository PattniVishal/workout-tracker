package com.workouttracker.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouttracker.common.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
class SecurityJsonResponseWriter {

    private final ObjectMapper objectMapper;

    SecurityJsonResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(status, code, message));
    }
}
