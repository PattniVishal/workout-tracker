package com.workouttracker.workout.history.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workouttracker.auth.api.LoginRequest;
import com.workouttracker.auth.api.RegisterRequest;
import com.workouttracker.exercise.api.CreateExerciseRequest;
import com.workouttracker.workout.session.infrastructure.WorkoutSessionRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkoutHistoryControllerTest {

    private static final UUID BENCH_PRESS_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SQUAT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkoutSessionRepository workoutSessionRepository;

    private MockHttpSession userASession;
    private MockHttpSession userBSession;

    @BeforeEach
    void setUpUsers() throws Exception {
        userASession = registerAndLogin("User A", "history-api-a@example.com");
        userBSession = registerAndLogin("User B", "history-api-b@example.com");
    }

    @Test
    void listsCompletedSessionsInReverseChronologicalOrder() throws Exception {
        String firstId = completeSession(userASession, "First");
        String secondId = completeSession(userASession, "Second");

        mockMvc.perform(get("/api/history").session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workouts.length()").value(2))
                .andExpect(jsonPath("$.workouts[0].id").value(secondId))
                .andExpect(jsonPath("$.workouts[1].id").value(firstId))
                .andExpect(jsonPath("$.workouts[0].name").value("Second"))
                .andExpect(jsonPath("$.workouts[0].completedAt").isNotEmpty())
                .andExpect(jsonPath("$.workouts[0].durationSeconds").isNumber())
                .andExpect(jsonPath("$.workouts[0].exerciseCount").value(1))
                .andExpect(jsonPath("$.workouts[0].completedSetCount").value(1));
    }

    @Test
    void listExcludesInProgressSession() throws Exception {
        completeSession(userASession, "Completed");
        startEmptySession(userASession);

        mockMvc.perform(get("/api/history").session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workouts.length()").value(1))
                .andExpect(jsonPath("$.workouts[0].name").value("Completed"));
    }

    @Test
    void listExcludesOtherUsersSessions() throws Exception {
        completeSession(userASession, "User A");

        mockMvc.perform(get("/api/history").session(userBSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workouts.length()").value(0));
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void returnsCompletedSessionDetail() throws Exception {
        String completedId = completeSession(userASession, "Push Day");

        mockMvc.perform(get("/api/history/{sessionId}", completedId).session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(completedId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.name").value("Push Day"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.durationSeconds").isNumber())
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("Bench Press"))
                .andExpect(jsonPath("$.exercises[0].position").value(1))
                .andExpect(jsonPath("$.exercises[0].sets[0].setNumber").value(1))
                .andExpect(jsonPath("$.exercises[0].sets[0].completed").value(true));
    }

    @Test
    void detailPreservesSnapshotAfterCatalogRename() throws Exception {
        String customExerciseId = createCustomExercise(userASession, "Snapshot Name");
        String completedId = completeSessionWithExercise(
                userASession,
                UUID.fromString(customExerciseId),
                "Custom Workout");
        renameCustomExercise(userASession, customExerciseId, "Renamed Exercise");

        mockMvc.perform(get("/api/history/{sessionId}", completedId).session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("Snapshot Name"));
    }

    @Test
    void detailReturnsNotFoundForUnknownSession() throws Exception {
        mockMvc.perform(get("/api/history/{sessionId}", UUID.randomUUID()).session(userASession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void detailReturnsNotFoundForAnotherUsersSession() throws Exception {
        String completedId = completeSession(userASession, "Private");

        mockMvc.perform(get("/api/history/{sessionId}", completedId).session(userBSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void detailReturnsNotFoundForInProgressSession() throws Exception {
        startEmptySession(userASession);
        String inProgressId = getCurrentSessionId(userASession);

        mockMvc.perform(get("/api/history/{sessionId}", inProgressId).session(userASession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void detailRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/history/{sessionId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void deletesCompletedSession() throws Exception {
        String completedId = completeSession(userASession, "Delete Me");

        mockMvc.perform(delete("/api/history/{sessionId}", completedId)
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/history/{sessionId}", completedId).session(userASession))
                .andExpect(status().isNotFound());

        assertThat(workoutSessionRepository.findById(UUID.fromString(completedId))).isEmpty();
    }

    @Test
    void deleteReturnsNotFoundWhenNoCompletedSession() throws Exception {
        mockMvc.perform(delete("/api/history/{sessionId}", UUID.randomUUID())
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void deleteReturnsNotFoundForInProgressSession() throws Exception {
        startEmptySession(userASession);
        String inProgressId = getCurrentSessionId(userASession);

        mockMvc.perform(delete("/api/history/{sessionId}", inProgressId)
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/sessions/current").session(userASession))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReturnsNotFoundForAnotherUsersSession() throws Exception {
        String completedId = completeSession(userASession, "User A");

        mockMvc.perform(delete("/api/history/{sessionId}", completedId)
                        .with(csrf())
                        .session(userBSession))
                .andExpect(status().isNotFound());

        assertThat(workoutSessionRepository.findById(UUID.fromString(completedId))).isPresent();
    }

    @Test
    void deleteRequiresCsrf() throws Exception {
        String completedId = completeSession(userASession, "CSRF");

        mockMvc.perform(delete("/api/history/{sessionId}", completedId).session(userASession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"))
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void previousPerformanceReturnsLatestCompletedWorkout() throws Exception {
        completeSessionWithWeight(userASession, "Older", 40);
        String latestSessionId = completeSessionWithWeight(userASession, "Newer", 55);

        mockMvc.perform(get("/api/exercises/{exerciseId}/previous-performance", BENCH_PRESS_ID)
                        .session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseId").value(BENCH_PRESS_ID.toString()))
                .andExpect(jsonPath("$.exerciseName").value("Bench Press"))
                .andExpect(jsonPath("$.sessionId").value(latestSessionId))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.sets.length()").value(1))
                .andExpect(jsonPath("$.sets[0].setNumber").value(1))
                .andExpect(jsonPath("$.sets[0].weightKg").value(55))
                .andExpect(jsonPath("$.sets[0].repetitions").value(10));
    }

    @Test
    void previousPerformanceReturnsNoContentWhenNoHistory() throws Exception {
        mockMvc.perform(get("/api/exercises/{exerciseId}/previous-performance", BENCH_PRESS_ID)
                        .session(userASession))
                .andExpect(status().isNoContent());
    }

    @Test
    void previousPerformanceReturnsNoContentForOtherExercise() throws Exception {
        completeSession(userASession, "Bench only");

        mockMvc.perform(get("/api/exercises/{exerciseId}/previous-performance", SQUAT_ID)
                        .session(userASession))
                .andExpect(status().isNoContent());
    }

    @Test
    void previousPerformanceReturnsNoContentForInProgressOnly() throws Exception {
        startEmptySession(userASession);
        addExercise(userASession, BENCH_PRESS_ID, 1);

        mockMvc.perform(get("/api/exercises/{exerciseId}/previous-performance", BENCH_PRESS_ID)
                        .session(userASession))
                .andExpect(status().isNoContent());
    }

    @Test
    void previousPerformanceDoesNotExposeOtherUsersData() throws Exception {
        completeSession(userASession, "User A");

        mockMvc.perform(get("/api/exercises/{exerciseId}/previous-performance", BENCH_PRESS_ID)
                        .session(userBSession))
                .andExpect(status().isNoContent());
    }

    @Test
    void previousPerformanceRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/exercises/{exerciseId}/previous-performance", BENCH_PRESS_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String completeSessionWithWeight(MockHttpSession session, String name, int weight) throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated());

        String workoutExerciseId = addExercise(session, BENCH_PRESS_ID, 1);
        completeFirstSet(session, workoutExerciseId, weight);

        MvcResult result = mockMvc.perform(post("/api/sessions/current/complete")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String completeSession(MockHttpSession session, String name) throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated());

        String workoutExerciseId = addExercise(session, BENCH_PRESS_ID, 1);
        completeFirstSet(session, workoutExerciseId);

        MvcResult result = mockMvc.perform(post("/api/sessions/current/complete")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String completeSessionWithExercise(MockHttpSession session, UUID exerciseId, String name) throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated());

        String workoutExerciseId = addExercise(session, exerciseId, 1);
        completeFirstSet(session, workoutExerciseId);

        MvcResult result = mockMvc.perform(post("/api/sessions/current/complete")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void startEmptySession(MockHttpSession session) throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ad hoc\"}"))
                .andExpect(status().isCreated());
    }

    private String addExercise(MockHttpSession session, UUID exerciseId, int initialSetCount) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("exerciseId", exerciseId.toString());
        request.put("initialSetCount", initialSetCount);

        MvcResult result = mockMvc.perform(post("/api/sessions/current/exercises")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("exercises").get(0).get("id").asText();
    }

    private void completeFirstSet(MockHttpSession session, String workoutExerciseId) throws Exception {
        completeFirstSet(session, workoutExerciseId, 50);
    }

    private void completeFirstSet(MockHttpSession session, String workoutExerciseId, int weight) throws Exception {
        MvcResult current = mockMvc.perform(get("/api/sessions/current").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String setId = objectMapper.readTree(current.getResponse().getContentAsString())
                .get("exercises").get(0).get("sets").get(0).get("id").asText();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("weightKg", weight);
        request.put("repetitions", 10);
        request.put("completed", true);

        mockMvc.perform(patch(
                        "/api/sessions/current/exercises/{workoutExerciseId}/sets/{setId}",
                        workoutExerciseId,
                        setId)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk());
    }

    private String getCurrentSessionId(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/sessions/current").session(session))
                .andExpect(status().isOk())
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

    private void renameCustomExercise(MockHttpSession session, String exerciseId, String newName) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("name", newName);
        request.put("primaryMuscleGroup", "Chest");
        request.putArray("secondaryMuscleGroups");
        request.put("category", "Machine");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/exercises/{exerciseId}", exerciseId)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk());
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
}
