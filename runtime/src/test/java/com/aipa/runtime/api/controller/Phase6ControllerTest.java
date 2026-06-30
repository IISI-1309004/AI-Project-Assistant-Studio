package com.aipa.runtime.api.controller;

import com.aipa.runtime.service.ExperienceEngineClient;
import com.aipa.runtime.service.WisdomEngineClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Phase 6 — WisdomController + ExperienceController 單元測試
 */
class Phase6ControllerTest {

    // ── WisdomController ─────────────────────────────────────────────

    @Test
    void shouldListWisdomRules() {
        WisdomEngineClient client = mock(WisdomEngineClient.class);
        when(client.listRules(anyString())).thenReturn(List.of(
                Map.of("id", "WIS-001", "title", "No direct repo in controller", "severity", "BLOCK"),
                Map.of("id", "WIS-002", "title", "No N+1 in loop", "severity", "WARN")
        ));
        WisdomController controller = new WisdomController(client);
        ResponseEntity<List<Map<String, Object>>> response = controller.listRules("");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("WIS-001", response.getBody().get(0).get("id"));
    }

    @Test
    void shouldAddWisdomRule() {
        WisdomEngineClient client = mock(WisdomEngineClient.class);
        when(client.addRule(any())).thenReturn(Map.of(
                "id", "WIS-CUSTOM-001",
                "title", "Custom Rule",
                "severity", "WARN"
        ));
        WisdomController controller = new WisdomController(client);
        ResponseEntity<Map<String, Object>> response = controller.addRule(Map.of(
                "title", "Custom Rule",
                "description", "A custom wisdom rule",
                "severity", "WARN"
        ));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("WIS-CUSTOM-001", response.getBody().get("id"));
    }

    @Test
    void shouldDetectBlockViolationOnCheck() {
        WisdomEngineClient client = mock(WisdomEngineClient.class);
        when(client.matchRules(any())).thenReturn(List.of(
                Map.of("id", "WIS-DB-001", "title", "No UPDATE without WHERE", "severity", "BLOCK")
        ));
        WisdomController controller = new WisdomController(client);
        ResponseEntity<Map<String, Object>> response = controller.checkForBlockViolations(Map.of(
                "code_diff", "UPDATE users SET status = 0",
                "file_names", List.of("UserMapper.xml"),
                "spec_type", "FEATURE"
        ));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Boolean.TRUE, response.getBody().get("hasBlockViolation"));
        assertEquals(1L, response.getBody().get("blockCount"));
    }

    @Test
    void shouldReturnNoViolationWhenRulesNotMatched() {
        WisdomEngineClient client = mock(WisdomEngineClient.class);
        when(client.matchRules(any())).thenReturn(List.of());
        WisdomController controller = new WisdomController(client);
        ResponseEntity<Map<String, Object>> response = controller.checkForBlockViolations(Map.of(
                "code_diff", "void greet() { return \"hello\"; }",
                "spec_type", "FEATURE"
        ));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Boolean.FALSE, response.getBody().get("hasBlockViolation"));
        assertEquals(0L, response.getBody().get("blockCount"));
    }

    // ── ExperienceController ─────────────────────────────────────────

    @Test
    void shouldReturnSimilarCases() {
        ExperienceEngineClient client = mock(ExperienceEngineClient.class);
        when(client.searchSimilar(anyString(), anyString(), anyInt())).thenReturn(List.of(
                Map.of("id", "exp-001", "title", "案件提醒功能", "_similarity", 0.87)
        ));
        ExperienceController controller = new ExperienceController(client);
        ResponseEntity<List<Map<String, Object>>> response = controller.searchSimilar(Map.of(
                "query", "案件提醒",
                "project_id", "demo",
                "top_k", 5
        ));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("exp-001", response.getBody().get(0).get("id"));
    }

    @Test
    void shouldReturnBadRequestWhenQueryMissing() {
        ExperienceEngineClient client = mock(ExperienceEngineClient.class);
        ExperienceController controller = new ExperienceController(client);
        ResponseEntity<List<Map<String, Object>>> response = controller.searchSimilar(Map.of(
                "project_id", "demo"
        ));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldListExperienceCases() {
        ExperienceEngineClient client = mock(ExperienceEngineClient.class);
        when(client.listCases(anyString())).thenReturn(List.of(
                Map.of("id", "exp-001", "title", "Feature A", "outcome", "SUCCESS"),
                Map.of("id", "exp-002", "title", "Feature B", "outcome", "PARTIAL")
        ));
        ExperienceController controller = new ExperienceController(client);
        ResponseEntity<List<Map<String, Object>>> response = controller.listCases("demo");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }
}

