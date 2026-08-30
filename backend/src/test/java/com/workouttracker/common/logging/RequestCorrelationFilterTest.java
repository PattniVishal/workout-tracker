package com.workouttracker.common.logging;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestCorrelationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatesRequestIdWhenMissing() throws Exception {
        var result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        assertNotNull(result.getResponse().getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER));
    }

    @Test
    void propagatesValidRequestIdWhenProvided() throws Exception {
        String requestId = "test-correlation-123";

        mockMvc.perform(get("/api/auth/csrf").header(RequestCorrelationFilter.REQUEST_ID_HEADER, requestId))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestCorrelationFilter.REQUEST_ID_HEADER, requestId));
    }

    @Test
    void ignoresInvalidRequestIdAndGeneratesNewValue() throws Exception {
        var result = mockMvc.perform(get("/api/auth/csrf").header(RequestCorrelationFilter.REQUEST_ID_HEADER, "bad id"))
                .andExpect(status().isOk())
                .andReturn();

        String responseRequestId = result.getResponse().getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);
        assertNotNull(responseRequestId);
        org.junit.jupiter.api.Assertions.assertNotEquals("bad id", responseRequestId);
    }
}
