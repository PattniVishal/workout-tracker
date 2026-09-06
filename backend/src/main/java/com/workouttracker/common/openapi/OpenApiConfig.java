package com.workouttracker.common.openapi;

import com.workouttracker.common.security.SecurityConfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SESSION_COOKIE_SCHEME = "sessionCookie";
    public static final String CSRF_HEADER_SCHEME = "csrfHeader";

    @Bean
    public OpenAPI workoutTrackerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Workout Tracker API")
                        .version("1.0.0")
                        .description("""
                                REST API for the Workout Tracker MVP.

                                ## Authentication
                                This API uses **server-side session authentication** with an HTTP-only session cookie \
                                (`JSESSIONID`). It does **not** use JWT or Bearer tokens.

                                1. Register (`POST /api/auth/register`) or login (`POST /api/auth/login`).
                                2. The server establishes a session and sets the session cookie.
                                3. The browser automatically sends the session cookie on subsequent requests.

                                ## CSRF protection
                                Unsafe HTTP methods (`POST`, `PUT`, `PATCH`, `DELETE`) require a CSRF token.

                                1. Call `GET /api/auth/csrf` to obtain the token and header name.
                                2. Send the token in the `X-XSRF-TOKEN` request header on unsafe requests.
                                3. The `XSRF-TOKEN` cookie is also set for browser clients.

                                Swagger UI in a browser can use the same session/cookie flow after logging in through \
                                the auth endpoints. For unsafe requests from Swagger UI, include the CSRF header \
                                according to the token returned by `/api/auth/csrf`.
                                """)
                        .contact(new Contact().name("Workout Tracker")))
                .addServersItem(new Server().url("http://localhost:8080").description("Local development"))
                .components(new Components()
                        .addSecuritySchemes(SESSION_COOKIE_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("HTTP-only session cookie established after successful login."))
                        .addSecuritySchemes(CSRF_HEADER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(SecurityConfig.CSRF_HEADER_NAME)
                                .description("CSRF token obtained from GET /api/auth/csrf. Required for unsafe methods.")))
                .addSecurityItem(new SecurityRequirement().addList(SESSION_COOKIE_SCHEME));
    }
}
