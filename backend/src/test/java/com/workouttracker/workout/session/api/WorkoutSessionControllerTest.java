package com.workouttracker.workout.session.api;

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
class WorkoutSessionControllerTest {

    private static final UUID BENCH_PRESS_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession userASession;
    private MockHttpSession userBSession;

    @BeforeEach
    void setUpUsers() throws Exception {
        userASession = registerAndLogin("User A", "session-api-a@example.com");
        userBSession = registerAndLogin("User B", "session-api-b@example.com");
    }

    @Test
    void startsEmptySession() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("name", "Ad hoc");

        mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Ad hoc"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.originRoutineId").doesNotExist())
                .andExpect(jsonPath("$.completedAt").doesNotExist())
                .andExpect(jsonPath("$.durationSeconds").doesNotExist())
                .andExpect(jsonPath("$.exercises").isArray())
                .andExpect(jsonPath("$.exercises.length()").value(0))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void startsSessionFromRoutine() throws Exception {
        String routineId = createRoutine(userASession, "Push Day");

        ObjectNode request = objectMapper.createObjectNode();
        request.put("routineId", routineId);

        mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Push Day"))
                .andExpect(jsonPath("$.originRoutineId").value(routineId))
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("Bench Press"))
                .andExpect(jsonPath("$.exercises[0].sets.length()").value(3))
                .andExpect(jsonPath("$.exercises[0].sets[0].completed").value(false));
    }

    @Test
    void returnsCurrentSessionForResume() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Resume Me\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionId = objectMapper.readTree(startResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/sessions/current").session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.name").value("Resume Me"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void returnsNoContentWhenNoActiveSession() throws Exception {
        mockMvc.perform(get("/api/sessions/current").session(userASession))
                .andExpect(status().isNoContent());
    }

    @Test
    void doesNotReturnAnotherUsersActiveSession() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"User A Session\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/sessions/current").session(userBSession))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsSecondStartWithConflict() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"First\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Second\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_SESSION_EXISTS"));
    }

    @Test
    void rejectsStartWithBothRoutineAndName() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("routineId", UUID.randomUUID().toString());
        request.put("name", "Invalid");

        mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsStartFromAnotherUsersRoutine() throws Exception {
        String routineId = createRoutine(userASession, "Private Routine");

        ObjectNode request = objectMapper.createObjectNode();
        request.put("routineId", routineId);

        mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(userBSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/sessions/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ad hoc\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsPostWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ad hoc\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"))
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    @Test
    void addExerciseReturnsUpdatedSession() throws Exception {
        startEmptySession(userASession);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("exerciseId", BENCH_PRESS_ID.toString());
        request.put("initialSetCount", 2);

        mockMvc.perform(post("/api/sessions/current/exercises")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises.length()").value(1))
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("Bench Press"))
                .andExpect(jsonPath("$.exercises[0].position").value(1))
                .andExpect(jsonPath("$.exercises[0].sets.length()").value(2));
    }

    @Test
    void addExerciseWithoutActiveSessionReturnsNotFound() throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("exerciseId", BENCH_PRESS_ID.toString());
        request.put("initialSetCount", 1);

        mockMvc.perform(post("/api/sessions/current/exercises")
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void removeExerciseReturnsUpdatedSession() throws Exception {
        startEmptySession(userASession);
        String workoutExerciseId = addExercise(userASession, BENCH_PRESS_ID, 2);

        mockMvc.perform(delete("/api/sessions/current/exercises/{workoutExerciseId}", workoutExerciseId)
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises.length()").value(0));
    }

    @Test
    void cannotRemoveAnotherUsersExercise() throws Exception {
        startEmptySession(userASession);
        String workoutExerciseId = addExercise(userASession, BENCH_PRESS_ID, 1);
        startEmptySession(userBSession);

        mockMvc.perform(delete("/api/sessions/current/exercises/{workoutExerciseId}", workoutExerciseId)
                        .with(csrf())
                        .session(userBSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void addSetReturnsUpdatedSession() throws Exception {
        startEmptySession(userASession);
        String workoutExerciseId = addExercise(userASession, BENCH_PRESS_ID, 1);

        mockMvc.perform(post("/api/sessions/current/exercises/{workoutExerciseId}/sets", workoutExerciseId)
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets.length()").value(2))
                .andExpect(jsonPath("$.exercises[0].sets[1].setNumber").value(2))
                .andExpect(jsonPath("$.exercises[0].sets[1].completed").value(false));
    }

    @Test
    void updateSetLogsValuesAndCompletion() throws Exception {
        startEmptySession(userASession);
        String workoutExerciseId = addExercise(userASession, BENCH_PRESS_ID, 1);
        String setId = getFirstSetId(userASession, workoutExerciseId);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("weightKg", 50);
        request.put("repetitions", 10);
        request.put("completed", true);

        mockMvc.perform(patch(
                        "/api/sessions/current/exercises/{workoutExerciseId}/sets/{setId}",
                        workoutExerciseId,
                        setId)
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets[0].weightKg").value(50))
                .andExpect(jsonPath("$.exercises[0].sets[0].repetitions").value(10))
                .andExpect(jsonPath("$.exercises[0].sets[0].completed").value(true));
    }

    @Test
    void updateSetRejectsCompletedWithoutValues() throws Exception {
        startEmptySession(userASession);
        String workoutExerciseId = addExercise(userASession, BENCH_PRESS_ID, 1);
        String setId = getFirstSetId(userASession, workoutExerciseId);

        mockMvc.perform(patch(
                        "/api/sessions/current/exercises/{workoutExerciseId}/sets/{setId}",
                        workoutExerciseId,
                        setId)
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void updateSetRejectsNegativeWeight() throws Exception {
        startEmptySession(userASession);
        String workoutExerciseId = addExercise(userASession, BENCH_PRESS_ID, 1);
        String setId = getFirstSetId(userASession, workoutExerciseId);

        mockMvc.perform(patch(
                        "/api/sessions/current/exercises/{workoutExerciseId}/sets/{setId}",
                        workoutExerciseId,
                        setId)
                        .with(csrf())
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void deleteSetReturnsUpdatedSessionWithRenumberedSets() throws Exception {
        startEmptySession(userASession);
        String workoutExerciseId = addExercise(userASession, BENCH_PRESS_ID, 3);
        String middleSetId = getSetIdAtIndex(userASession, workoutExerciseId, 1);

        mockMvc.perform(delete(
                        "/api/sessions/current/exercises/{workoutExerciseId}/sets/{setId}",
                        workoutExerciseId,
                        middleSetId)
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets.length()").value(2))
                .andExpect(jsonPath("$.exercises[0].sets[0].setNumber").value(1))
                .andExpect(jsonPath("$.exercises[0].sets[1].setNumber").value(2));
    }

    @Test
    void loggingEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/sessions/current/exercises")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + BENCH_PRESS_ID + "\",\"initialSetCount\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void loggingMutationsRequireCsrf() throws Exception {
        startEmptySession(userASession);

        mockMvc.perform(post("/api/sessions/current/exercises")
                        .session(userASession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + BENCH_PRESS_ID + "\",\"initialSetCount\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void completeReturnsCompletedSession() throws Exception {
        startEmptySession(userASession);
        String workoutExerciseId = addExercise(userASession, BENCH_PRESS_ID, 1);
        completeFirstSet(userASession, workoutExerciseId);

        mockMvc.perform(post("/api/sessions/current/complete")
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.durationSeconds").isNumber());

        mockMvc.perform(get("/api/sessions/current").session(userASession))
                .andExpect(status().isNoContent());
    }

    @Test
    void completeWithoutCompletedSetsReturnsConflict() throws Exception {
        startEmptySession(userASession);
        addExercise(userASession, BENCH_PRESS_ID, 1);

        mockMvc.perform(post("/api/sessions/current/complete")
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_COMPLETED_SETS"));
    }

    @Test
    void completeWithoutActiveSessionReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/sessions/current/complete")
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void completeRequiresCsrf() throws Exception {
        startEmptySession(userASession);

        mockMvc.perform(post("/api/sessions/current/complete").session(userASession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void discardRemovesCurrentSession() throws Exception {
        startEmptySession(userASession);
        addExercise(userASession, BENCH_PRESS_ID, 2);

        mockMvc.perform(delete("/api/sessions/current")
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/sessions/current").session(userASession))
                .andExpect(status().isNoContent());
    }

    @Test
    void discardWithoutActiveSessionReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/sessions/current")
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void discardRequiresCsrf() throws Exception {
        startEmptySession(userASession);

        mockMvc.perform(delete("/api/sessions/current").session(userASession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void anotherUserCannotDiscardSession() throws Exception {
        startEmptySession(userASession);

        mockMvc.perform(delete("/api/sessions/current")
                        .with(csrf())
                        .session(userBSession))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/sessions/current").session(userASession))
                .andExpect(status().isOk());
    }

    private void completeFirstSet(MockHttpSession session, String workoutExerciseId) throws Exception {
        String setId = getFirstSetId(session, workoutExerciseId);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("weightKg", 50);
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

    private String getFirstSetId(MockHttpSession session, String workoutExerciseId) throws Exception {
        return getSetIdAtIndex(session, workoutExerciseId, 0);
    }

    private String getSetIdAtIndex(MockHttpSession session, String workoutExerciseId, int index) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/sessions/current").session(session))
                .andExpect(status().isOk())
                .andReturn();

        var exercises = objectMapper.readTree(result.getResponse().getContentAsString()).get("exercises");
        for (var exercise : exercises) {
            if (workoutExerciseId.equals(exercise.get("id").asText())) {
                return exercise.get("sets").get(index).get("id").asText();
            }
        }
        throw new IllegalStateException("Workout exercise not found in session");
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
        ObjectNode request = objectMapper.createObjectNode();
        request.put("name", name);
        request.putArray("exercises")
                .addObject()
                .put("exerciseId", BENCH_PRESS_ID.toString())
                .put("plannedSetCount", 3);

        MvcResult result = mockMvc.perform(post("/api/routines")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
