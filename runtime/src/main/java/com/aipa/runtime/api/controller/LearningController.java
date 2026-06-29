package com.aipa.runtime.api.controller;

import com.aipa.runtime.service.LearningEngineClient;
import com.aipa.runtime.service.SessionWorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/learn")
public class LearningController {

    private final LearningEngineClient learningEngineClient;
    private final SessionWorkflowService sessionWorkflowService;

    public LearningController(LearningEngineClient learningEngineClient, SessionWorkflowService sessionWorkflowService) {
        this.learningEngineClient = learningEngineClient;
        this.sessionWorkflowService = sessionWorkflowService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> analyze(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(learningEngineClient.analyze(body));
    }

    @GetMapping("/{learningId}")
    public ResponseEntity<Map<String, Object>> getResult(@PathVariable String learningId) {
        return ResponseEntity.ok(learningEngineClient.getResult(learningId));
    }

    @PostMapping("/{learningId}/rollback")
    public ResponseEntity<Map<String, Object>> rollback(@PathVariable String learningId) {
        return ResponseEntity.ok(learningEngineClient.rollback(learningId));
    }

    /**
     * Phase 5-2: 回寫 learning result 到 session
     */
    @PostMapping("/{learningId}/write-back")
    public ResponseEntity<Map<String, Object>> writeBackToSession(
            @PathVariable String learningId,
            @RequestParam(required = false) String sessionId) {
        try {
            Map<String, Object> learningResult = learningEngineClient.getResult(learningId);
            Map<String, Object> updatedSession = null;
            if (sessionId != null && !sessionId.isBlank()) {
                updatedSession = sessionWorkflowService.writeLearningResultToSession(sessionId, learningId, learningResult);
            }
            return ResponseEntity.ok(Map.of(
                    "message", "Learning result written back",
                    "learningId", learningId,
                    "result", learningResult,
                    "session", updatedSession == null ? Map.of() : updatedSession
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to write back learning result",
                    "message", ex.getMessage()
            ));
        }
    }
}

