package com.workouttracker.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouttracker.support.SecurityTestController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(SecurityTestController.class)
class AuthMeAndLogoutControllerTest {

    private static final String EMAIL = "me-logout@example.com";
    private static final String PASSWORD = "secret-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void registerUser() throws Exception {
        RegisterRequest request = new RegisterRequest("Vishal", EMAIL, PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void returnsCurrentUserForAuthenticatedRequest() throws Exception {
        MockHttpSession session = loginAndGetSession();

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.displayName").value("Vishal"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void returnsUnauthorizedForUnauthenticatedMeRequest() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required."))
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void logoutSucceedsForAuthenticatedUser() throws Exception {
        MockHttpSession session = loginAndGetSession();

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsLogoutWithoutCsrfToken() throws Exception {
        MockHttpSession session = loginAndGetSession();

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void invalidatesSessionAfterLogout() throws Exception {
        MockHttpSession session = loginAndGetSession();

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/test/protected").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void returnsUnauthorizedForLogoutWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void completeAuthenticationLifecycle() throws Exception {
        MockHttpSession session = loginAndGetSession();

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/test/protected").session(session))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession loginAndGetSession() throws Exception {
        LoginRequest request = new LoginRequest(EMAIL, PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession();
    }
}
