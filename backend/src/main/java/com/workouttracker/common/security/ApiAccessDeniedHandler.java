package com.workouttracker.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityJsonResponseWriter responseWriter;

    public ApiAccessDeniedHandler(SecurityJsonResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        if (isCsrfFailure(accessDeniedException)) {
            responseWriter.write(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "CSRF_TOKEN_INVALID",
                    "CSRF token is missing or invalid."
            );
            return;
        }

        responseWriter.write(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "FORBIDDEN",
                "Access is denied."
        );
    }

    private boolean isCsrfFailure(AccessDeniedException accessDeniedException) {
        return accessDeniedException instanceof MissingCsrfTokenException
                || accessDeniedException instanceof InvalidCsrfTokenException
                || accessDeniedException.getCause() instanceof MissingCsrfTokenException
                || accessDeniedException.getCause() instanceof InvalidCsrfTokenException;
    }
}
