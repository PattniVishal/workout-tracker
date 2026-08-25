package com.workouttracker.workout.routine.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workouttracker.auth.api.LoginRequest;
import com.workouttracker.auth.api.RegisterRequest;
import com.workouttracker.exercise.infrastructure.ExerciseRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkoutRoutineControllerTest {

    private static final UUID BENCH_PRESS_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SQUAT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExerciseRepository exerciseRepository;

    private MockHttpSession userASession;
    private MockHttpSession userBSession;

    @BeforeEach
    void setUpUsers() throws Exception {
        userASession = registerAndLogin("User A", "routine-api-a@example.com");
        userBSession = registerAndLogin("User B", "routine-api-b@example.com");
    }

    @Test
    void createsRoutineForAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/routines")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRoutinePayload("Push Day", "Chest", BENCH_PRESS_ID, 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Push Day"))
                .andExpect(jsonPath("$.description").value("Chest"))
                .andExpect(jsonPath("$.exercises.length()").value(1))
                .andExpect(jsonPath("$.exercises[0].exerciseId").value(BENCH_PRESS_ID.toString()))
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("Bench Press"))
                .andExpect(jsonPath("$.exercises[0].plannedSetCount").value(3))
                .andExpect(jsonPath("$.exercises[0].position").value(1))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void rejectsUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/routines"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/routines")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRoutinePayload("Push Day", null, BENCH_PRESS_ID, 3)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsValidationErrorForInvalidCreateRequest() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("name", "");
        request.putArray("exercises");

        mockMvc.perform(post("/api/routines")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsDuplicateExerciseIds() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("name", "Push Day");
        ArrayNode exercises = request.putArray("exercises");
        exercises.addObject()
                .put("exerciseId", BENCH_PRESS_ID.toString())
                .put("plannedSetCount", 3);
        exercises.addObject()
                .put("exerciseId", BENCH_PRESS_ID.toString())
                .put("plannedSetCount", 4);

        mockMvc.perform(post("/api/routines")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ROUTINE_EXERCISE"));
    }

    @Test
    void rejectsNonPickableExerciseReference() throws Exception {
        String customExerciseId = createCustomExercise(userASession, "Archived Custom");

        mockMvc.perform(post("/api/exercises/" + customExerciseId + "/archive")
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/routines")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRoutinePayload("Push Day", null, UUID.fromString(customExerciseId), 3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EXERCISE_NOT_PICKABLE"));
    }

    @Test
    void listsOnlyOwnRoutinesInApprovedOrder() throws Exception {
        String routineAId = createRoutine(userASession, "User A Routine");
        createRoutine(userBSession, "User B Routine");

        mockMvc.perform(get("/api/routines").session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routines.length()").value(1))
                .andExpect(jsonPath("$.routines[0].id").value(routineAId))
                .andExpect(jsonPath("$.routines[0].name").value("User A Routine"))
                .andExpect(jsonPath("$.routines[0].exerciseCount").value(1))
                .andExpect(jsonPath("$.routines[0].updatedAt").isNotEmpty());
    }

    @Test
    void returnsRoutineDetailWithOrderedExercises() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("name", "Full Body");
        ArrayNode exercises = request.putArray("exercises");
        exercises.addObject().put("exerciseId", SQUAT_ID.toString()).put("plannedSetCount", 5);
        exercises.addObject().put("exerciseId", BENCH_PRESS_ID.toString()).put("plannedSetCount", 3);

        MvcResult createResult = mockMvc.perform(post("/api/routines")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isCreated())
                .andReturn();

        String routineId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/routines/{routineId}", routineId).session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].position").value(1))
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("Squat"))
                .andExpect(jsonPath("$.exercises[1].position").value(2))
                .andExpect(jsonPath("$.exercises[1].exerciseName").value("Bench Press"));
    }

    @Test
    void ownerCanUpdateRoutineAndOtherUserCannot() throws Exception {
        String routineId = createRoutine(userASession, "Original");

        mockMvc.perform(put("/api/routines/{routineId}", routineId)
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRoutinePayload("Updated", "New description", SQUAT_ID, 4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.exercises[0].exerciseId").value(SQUAT_ID.toString()));

        mockMvc.perform(put("/api/routines/{routineId}", routineId)
                        .with(csrf())
                        .session(userBSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRoutinePayload("Hijacked", null, BENCH_PRESS_ID, 3)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void ownerCanDeleteRoutineAndExercisesRemainInCatalog() throws Exception {
        long exerciseCountBefore = exerciseRepository.count();
        String routineId = createRoutine(userASession, "Delete Me");

        mockMvc.perform(delete("/api/routines/{routineId}", routineId)
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/routines/{routineId}", routineId).session(userASession))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/routines/{routineId}", routineId)
                        .with(csrf())
                        .session(userBSession))
                .andExpect(status().isNotFound());

        assertThat(exerciseRepository.count()).isEqualTo(exerciseCountBefore);
    }

    @Test
    void rejectsUnsafeRequestsWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/routines")
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRoutinePayload("Push Day", null, BENCH_PRESS_ID, 3)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"))
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void allowsOwnActiveCustomExerciseInRoutine() throws Exception {
        String customExerciseId = createCustomExercise(userASession, "Cable Fly");

        mockMvc.perform(post("/api/routines")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRoutinePayload("Custom Routine", null, UUID.fromString(customExerciseId), 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("Cable Fly"));
    }

    @Test
    void otherUserCannotSeeOwnersCustomExerciseRoutineReference() throws Exception {
        String customExerciseId = createCustomExercise(userASession, "Private Exercise");
        createRoutine(userASession, "Private Routine", UUID.fromString(customExerciseId));

        mockMvc.perform(post("/api/routines")
                        .with(csrf())
                        .session(userBSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRoutinePayload("Should Fail", null, UUID.fromString(customExerciseId), 3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EXERCISE_NOT_PICKABLE"));
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

    private String createRoutine(MockHttpSession session, String name) throws Exception {
        return createRoutine(session, name, BENCH_PRESS_ID);
    }

    private String createRoutine(MockHttpSession session, String name, UUID exerciseId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/routines")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRoutinePayload(name, null, exerciseId, 3)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createCustomExercise(MockHttpSession session, String name) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("name", name);
        request.put("primaryMuscleGroup", "Chest");
        request.putArray("secondaryMuscleGroups");
        request.put("category", "Machine");

        MvcResult result = mockMvc.perform(post("/api/exercises")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createRoutinePayload(String name, String description, UUID exerciseId, int plannedSetCount)
            throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("name", name);
        if (description != null) {
            request.put("description", description);
        }
        request.putArray("exercises")
                .addObject()
                .put("exerciseId", exerciseId.toString())
                .put("plannedSetCount", plannedSetCount);
        return request.toString();
    }
}
