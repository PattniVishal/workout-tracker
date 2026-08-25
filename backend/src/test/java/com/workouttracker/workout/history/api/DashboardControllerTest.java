package com.workouttracker.workout.history.api;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardControllerTest {

    private static final UUID BENCH_PRESS_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession userASession;
    private MockHttpSession userBSession;

    @BeforeEach
    void setUpUsers() throws Exception {
        userASession = registerAndLogin("User A", "dashboard-api-a@example.com");
        userBSession = registerAndLogin("User B", "dashboard-api-b@example.com");
    }

    @Test
    void returnsEmptyDashboardForNewUser() throws Exception {
        mockMvc.perform(get("/api/dashboard").session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("User A"))
                .andExpect(jsonPath("$.hasActiveSession").value(false))
                .andExpect(jsonPath("$.activeSessionId").doesNotExist())
                .andExpect(jsonPath("$.routines.length()").value(0))
                .andExpect(jsonPath("$.mostRecentCompleted").doesNotExist())
                .andExpect(jsonPath("$.totalCompletedWorkouts").value(0))
                .andExpect(jsonPath("$.totalCompletedSets").value(0));
    }

    @Test
    void returnsRoutineSummariesAndActiveSession() throws Exception {
        createRoutine(userASession, "Push Day");
        String activeSessionId = startEmptySession(userASession);

        mockMvc.perform(get("/api/dashboard").session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasActiveSession").value(true))
                .andExpect(jsonPath("$.activeSessionId").value(activeSessionId))
                .andExpect(jsonPath("$.routines.length()").value(1))
                .andExpect(jsonPath("$.routines[0].name").value("Push Day"))
                .andExpect(jsonPath("$.routines[0].exerciseCount").value(1));
    }

    @Test
    void returnsCompletedHistorySummary() throws Exception {
        completeSession(userASession, "Completed Workout");

        mockMvc.perform(get("/api/dashboard").session(userASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompletedWorkouts").value(1))
                .andExpect(jsonPath("$.totalCompletedSets").value(1))
                .andExpect(jsonPath("$.mostRecentCompleted.name").value("Completed Workout"))
                .andExpect(jsonPath("$.mostRecentCompleted.completedAt").isNotEmpty());
    }

    @Test
    void doesNotExposeAnotherUsersDashboardData() throws Exception {
        createRoutine(userASession, "Private Routine");
        completeSession(userASession, "Private Workout");

        mockMvc.perform(get("/api/dashboard").session(userBSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("User B"))
                .andExpect(jsonPath("$.routines.length()").value(0))
                .andExpect(jsonPath("$.mostRecentCompleted").doesNotExist())
                .andExpect(jsonPath("$.totalCompletedWorkouts").value(0))
                .andExpect(jsonPath("$.totalCompletedSets").value(0));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.fieldErrors", empty()));
    }

    private void createRoutine(MockHttpSession session, String name) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("name", name);
        request.putArray("exercises")
                .addObject()
                .put("exerciseId", BENCH_PRESS_ID.toString())
                .put("plannedSetCount", 3);

        mockMvc.perform(post("/api/routines")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isCreated());
    }

    private String startEmptySession(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Active\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void completeSession(MockHttpSession session, String name) throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated());

        ObjectNode addExercise = objectMapper.createObjectNode();
        addExercise.put("exerciseId", BENCH_PRESS_ID.toString());
        addExercise.put("initialSetCount", 1);

        mockMvc.perform(post("/api/sessions/current/exercises")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addExercise.toString()))
                .andExpect(status().isOk());

        MvcResult current = mockMvc.perform(get("/api/sessions/current").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String workoutExerciseId = objectMapper.readTree(current.getResponse().getContentAsString())
                .get("exercises").get(0).get("id").asText();
        String setId = objectMapper.readTree(current.getResponse().getContentAsString())
                .get("exercises").get(0).get("sets").get(0).get("id").asText();

        ObjectNode updateSet = objectMapper.createObjectNode();
        updateSet.put("weightKg", 50);
        updateSet.put("repetitions", 10);
        updateSet.put("completed", true);

        mockMvc.perform(patch(
                        "/api/sessions/current/exercises/{workoutExerciseId}/sets/{setId}",
                        workoutExerciseId,
                        setId)
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateSet.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/sessions/current/complete")
                        .with(csrf())
                        .session(session))
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
