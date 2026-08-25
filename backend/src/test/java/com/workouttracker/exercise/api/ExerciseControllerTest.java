package com.workouttracker.exercise.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouttracker.auth.api.LoginRequest;
import com.workouttracker.auth.api.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExerciseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;

    @BeforeEach
    void authenticate() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Vishal", "exercise-api@example.com", "secret-password");
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("exercise-api@example.com", "secret-password"))))
                .andExpect(status().isOk())
                .andReturn();

        session = (MockHttpSession) loginResult.getRequest().getSession();
    }

    @Test
    void returnsApprovedExerciseListForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/exercises").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises.length()").value(6))
                .andExpect(jsonPath("$.exercises[0].id").isNotEmpty())
                .andExpect(jsonPath("$.exercises[0].name").value("Barbell Row"))
                .andExpect(jsonPath("$.exercises[1].name").value("Bench Press"))
                .andExpect(jsonPath("$.exercises[1].primaryMuscleGroup").value("Chest"))
                .andExpect(jsonPath("$.exercises[1].secondaryMuscleGroups[0]").value("Triceps"))
                .andExpect(jsonPath("$.exercises[1].secondaryMuscleGroups[1]").value("Shoulders"))
                .andExpect(jsonPath("$.exercises[1].category").value("Barbell"))
                .andExpect(jsonPath("$.exercises[1].source").value("SYSTEM"))
                .andExpect(jsonPath("$.exercises[1].createdByUserId").doesNotExist())
                .andExpect(jsonPath("$.exercises[1].archivedAt").doesNotExist())
                .andExpect(jsonPath("$.exercises[1].createdAt").doesNotExist())
                .andExpect(jsonPath("$.exercises[1].updatedAt").doesNotExist());
    }

    @Test
    void supportsApprovedQueryFilters() throws Exception {
        mockMvc.perform(get("/api/exercises")
                        .param("q", "press")
                        .param("muscleGroup", "Chest")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises.length()").value(1))
                .andExpect(jsonPath("$.exercises[0].name").value("Bench Press"));
    }

    @Test
    void supportsQueryFilterOnly() throws Exception {
        mockMvc.perform(get("/api/exercises")
                        .param("q", "press")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises.length()").value(2))
                .andExpect(jsonPath("$.exercises[0].name").value("Bench Press"))
                .andExpect(jsonPath("$.exercises[1].name").value("Overhead Press"));
    }

    @Test
    void supportsCaseInsensitiveQueryFilter() throws Exception {
        mockMvc.perform(get("/api/exercises")
                        .param("q", "bench")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises.length()").value(1))
                .andExpect(jsonPath("$.exercises[0].name").value("Bench Press"));
    }

    @Test
    void supportsMuscleGroupFilterOnly() throws Exception {
        mockMvc.perform(get("/api/exercises")
                        .param("muscleGroup", "Back")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises.length()").value(3))
                .andExpect(jsonPath("$.exercises[0].name").value("Barbell Row"));
    }

    @Test
    void returnsAllPickableExercisesWhenOptionalFiltersAreAbsent() throws Exception {
        mockMvc.perform(get("/api/exercises").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises.length()").value(6));
    }

    @Test
    void returnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required."))
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }
}
