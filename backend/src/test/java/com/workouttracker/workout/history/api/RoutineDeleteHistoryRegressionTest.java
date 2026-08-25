package com.workouttracker.workout.history.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workouttracker.auth.api.LoginRequest;
import com.workouttracker.auth.api.RegisterRequest;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
class RoutineDeleteHistoryRegressionTest {

    private static final UUID BENCH_PRESS_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkoutSessionRepository workoutSessionRepository;

    private MockHttpSession session;

    @BeforeEach
    void setUpUser() throws Exception {
        session = registerAndLogin("Vishal", "routine-delete-history@example.com");
    }

    @Test
    void completedHistoryRemainsAccessibleAfterRoutineDelete() throws Exception {
        String routineId = createRoutine(session, "Push Day");
        String completedSessionId = completeSessionFromRoutine(session, routineId, "Push Day");

        assertThat(workoutSessionRepository.findById(UUID.fromString(completedSessionId)))
                .isPresent()
                .get()
                .satisfies(workoutSession -> assertThat(workoutSession.getOriginRoutineId())
                        .isEqualTo(UUID.fromString(routineId)));

        mockMvc.perform(delete("/api/routines/{routineId}", routineId)
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent());

        assertThat(workoutSessionRepository.findById(UUID.fromString(completedSessionId)))
                .isPresent()
                .get()
                .satisfies(workoutSession -> assertThat(workoutSession.getOriginRoutineId()).isNull());

        mockMvc.perform(get("/api/history/{sessionId}", completedSessionId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(completedSessionId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.name").value("Push Day"))
                .andExpect(jsonPath("$.originRoutineId").doesNotExist())
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("Bench Press"))
                .andExpect(jsonPath("$.exercises[0].sets[0].completed").value(true))
                .andExpect(jsonPath("$.exercises[0].sets[0].weightKg").value(50))
                .andExpect(jsonPath("$.exercises[0].sets[0].repetitions").value(10));

        mockMvc.perform(get("/api/history").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workouts.length()").value(1))
                .andExpect(jsonPath("$.workouts[0].id").value(completedSessionId))
                .andExpect(jsonPath("$.workouts[0].name").value("Push Day"));
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

    private String completeSessionFromRoutine(MockHttpSession session, String routineId, String expectedName)
            throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routineId\":\"" + routineId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(expectedName))
                .andExpect(jsonPath("$.originRoutineId").value(routineId));

        MvcResult current = mockMvc.perform(get("/api/sessions/current").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String sessionId = objectMapper.readTree(current.getResponse().getContentAsString()).get("id").asText();
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        return sessionId;
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
