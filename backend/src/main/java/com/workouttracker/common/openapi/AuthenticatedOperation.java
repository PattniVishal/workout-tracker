package com.workouttracker.common.openapi;

import com.workouttracker.common.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SecurityRequirement(name = OpenApiConfig.SESSION_COOKIE_SCHEME)
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Authentication is required",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "403",
                description = "Access denied or CSRF token missing/invalid",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
        )
})
public @interface AuthenticatedOperation {
}
