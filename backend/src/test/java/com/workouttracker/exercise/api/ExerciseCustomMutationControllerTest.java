package com.workouttracker.exercise.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.util.UUID;

import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExerciseCustomMutationControllerTest {

    private static final UUID SYSTEM_BENCH_PRESS_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession userASession;
    private MockHttpSession userBSession;

    @BeforeEach
    void setUpUsers() throws Exception {
        userASession = registerAndLogin("User A", "custom-mutation-a@example.com");
        userBSession = registerAndLogin("User B", "custom-mutation-b@example.com");
    }

    @Test
    void createsCustomExerciseForAuthenticatedUser() throws Exception {
        CreateExerciseRequest request = new CreateExerciseRequest(
                "Smith Machine Incline Press",
                "Chest",
                java.util.List.of("Triceps"),
                "Machine"
        );

        mockMvc.perform(post("/api/exercises")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Smith Machine Incline Press"))
                .andExpect(jsonPath("$.primaryMuscleGroup").value("Chest"))
                .andExpect(jsonPath("$.secondaryMuscleGroups[0]").value("Triceps"))
                .andExpect(jsonPath("$.category").value("Machine"))
                .andExpect(jsonPath("$.source").value("CUSTOM"))
                .andExpect(jsonPath("$.createdByUserId").doesNotExist())
                .andExpect(jsonPath("$.archivedAt").doesNotExist());
    }

    @Test
    void rejectsCreateWithoutAuthentication() throws Exception {
        CreateExerciseRequest request = new CreateExerciseRequest(
                "Smith Machine Incline Press",
                "Chest",
                java.util.List.of("Triceps"),
                "Machine"
        );

        mockMvc.perform(post("/api/exercises")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void returnsValidationErrorForInvalidCreateRequest() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("name", "");
        request.put("primaryMuscleGroup", "Chest");
        request.putArray("secondaryMuscleGroups");
        request.put("category", "Machine");

        mockMvc.perform(post("/api/exercises")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request is invalid."))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void ignoresClientSuppliedOwnershipAndSourceFields() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("name", "Client Owned");
        request.put("primaryMuscleGroup", "Chest");
        request.putArray("secondaryMuscleGroups");
        request.put("category", "Machine");
        request.put("createdByUserId", UUID.randomUUID().toString());
        request.put("source", "SYSTEM");

        MvcResult result = mockMvc.perform(post("/api/exercises")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("CUSTOM"))
                .andReturn();

        String exerciseId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/exercises").session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[?(@.id == '" + exerciseId + "')].source").value("CUSTOM"));
    }

    @Test
    void ownerCanUpdateOwnCustomExercise() throws Exception {
        String exerciseId = createCustomExercise(userASession, "Cable Fly", "Chest", "Cable");

        CreateExerciseRequest updateRequest = new CreateExerciseRequest(
                "Cable Fly Updated",
                "Chest",
                java.util.List.of("Shoulders"),
                "Cable"
        );

        mockMvc.perform(put("/api/exercises/{exerciseId}", exerciseId)
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exerciseId))
                .andExpect(jsonPath("$.name").value("Cable Fly Updated"))
                .andExpect(jsonPath("$.secondaryMuscleGroups[0]").value("Shoulders"))
                .andExpect(jsonPath("$.source").value("CUSTOM"));
    }

    @Test
    void otherUserCannotUpdateCustomExercise() throws Exception {
        String exerciseId = createCustomExercise(userASession, "Private Exercise", "Chest", "Machine");

        CreateExerciseRequest updateRequest = new CreateExerciseRequest(
                "Hijacked",
                "Chest",
                java.util.List.of(),
                "Machine"
        );

        mockMvc.perform(put("/api/exercises/{exerciseId}", exerciseId)
                        .with(csrf())
                        .session(userBSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void cannotUpdateSystemExercise() throws Exception {
        CreateExerciseRequest updateRequest = new CreateExerciseRequest(
                "Bench Press Renamed",
                "Chest",
                java.util.List.of("Triceps", "Shoulders"),
                "Barbell"
        );

        mockMvc.perform(put("/api/exercises/{exerciseId}", SYSTEM_BENCH_PRESS_ID)
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void returnsNotFoundForUnknownExerciseOnUpdate() throws Exception {
        CreateExerciseRequest updateRequest = new CreateExerciseRequest(
                "Missing",
                "Chest",
                java.util.List.of(),
                "Machine"
        );

        mockMvc.perform(put("/api/exercises/{exerciseId}", UUID.randomUUID())
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void ownerCanUpdateArchivedCustomExercise() throws Exception {
        String exerciseId = createCustomExercise(userASession, "Archived Custom", "Back", "Machine");

        mockMvc.perform(post("/api/exercises/{exerciseId}/archive", exerciseId)
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isNoContent());

        CreateExerciseRequest updateRequest = new CreateExerciseRequest(
                "Archived Custom Renamed",
                "Back",
                java.util.List.of("Biceps"),
                "Machine"
        );

        mockMvc.perform(put("/api/exercises/{exerciseId}", exerciseId)
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Archived Custom Renamed"));
    }

    @Test
    void ownerCanArchiveOwnCustomExercise() throws Exception {
        String exerciseId = createCustomExercise(userASession, "To Archive", "Legs", "Machine");

        mockMvc.perform(post("/api/exercises/{exerciseId}/archive", exerciseId)
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/exercises").session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[?(@.id == '" + exerciseId + "')]").doesNotExist());
    }

    @Test
    void archiveIsIdempotentForOwner() throws Exception {
        String exerciseId = createCustomExercise(userASession, "Archive Twice", "Legs", "Machine");

        mockMvc.perform(post("/api/exercises/{exerciseId}/archive", exerciseId)
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/exercises/{exerciseId}/archive", exerciseId)
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isNoContent());
    }

    @Test
    void otherUserCannotArchiveCustomExercise() throws Exception {
        String exerciseId = createCustomExercise(userASession, "Protected Exercise", "Chest", "Machine");

        mockMvc.perform(post("/api/exercises/{exerciseId}/archive", exerciseId)
                        .with(csrf())
                        .session(userBSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void cannotArchiveSystemExercise() throws Exception {
        mockMvc.perform(post("/api/exercises/{exerciseId}/archive", SYSTEM_BENCH_PRESS_ID)
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void ownerSeesOwnActiveCustomExerciseButOtherUserDoesNot() throws Exception {
        String exerciseId = createCustomExercise(userASession, "User A Custom", "Chest", "Machine");

        mockMvc.perform(get("/api/exercises").session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[?(@.id == '" + exerciseId + "')].name")
                        .value("User A Custom"));

        mockMvc.perform(get("/api/exercises").session(userBSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[?(@.id == '" + exerciseId + "')]").doesNotExist());
    }

    @Test
    void rejectsUnsafeRequestsWithoutCsrfToken() throws Exception {
        CreateExerciseRequest request = new CreateExerciseRequest(
                "No CSRF",
                "Chest",
                java.util.List.of(),
                "Machine"
        );

        mockMvc.perform(post("/api/exercises")
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"))
                .andExpect(jsonPath("$.fieldErrors", empty()));

        mockMvc.perform(put("/api/exercises/{exerciseId}", UUID.randomUUID())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    private MockHttpSession registerAndLogin(String displayName, String email) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(displayName, email, "secret-password");
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "secret-password"))))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession();
    }

    private String createCustomExercise(MockHttpSession session, String name, String muscleGroup, String category)
            throws Exception {
        CreateExerciseRequest request = new CreateExerciseRequest(
                name,
                muscleGroup,
                java.util.List.of(),
                category
        );

        MvcResult result = mockMvc.perform(post("/api/exercises")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
