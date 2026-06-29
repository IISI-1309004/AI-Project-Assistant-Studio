package com.aipa.runtime.api.controller;

import com.aipa.runtime.service.SessionWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionControllerTest {

    @Test
    void shouldReturnMemoryReinforcementStatus() {
        SessionWorkflowService service = mock(SessionWorkflowService.class);
        when(service.getMemoryReinforcementStatus("s-1")).thenReturn(Map.of(
                "sessionId", "s-1",
                "status", "AVAILABLE",
                "memoryReinforcement", Map.of("reinforced", 2, "attempted", 2)
        ));

        SessionController controller = new SessionController(service);
        ResponseEntity<Map<String, Object>> response = controller.getMemoryReinforcement("s-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AVAILABLE", response.getBody().get("status"));
    }

    @Test
    void shouldReturnNotFoundWhenMemoryReinforcementSessionMissing() {
        SessionWorkflowService service = mock(SessionWorkflowService.class);
        when(service.getMemoryReinforcementStatus("s-missing"))
                .thenThrow(new IllegalArgumentException("Session not found: s-missing"));

        SessionController controller = new SessionController(service);
        ResponseEntity<Map<String, Object>> response = controller.getMemoryReinforcement("s-missing");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

