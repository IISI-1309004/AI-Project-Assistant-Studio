package com.aipa.runtime.api.controller;

import com.aipa.runtime.service.SessionWorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/session")
public class SessionController {

    private final SessionWorkflowService sessionWorkflowService;

    public SessionController(SessionWorkflowService sessionWorkflowService) {
        this.sessionWorkflowService = sessionWorkflowService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(@RequestBody Map<String, String> body) {
        Map<String, Object> session = sessionWorkflowService.createSession(
                body.getOrDefault("projectId", "default"),
                body.getOrDefault("projectRoot", System.getProperty("user.dir")),
                body.getOrDefault("requirement", "")
        );
        return ResponseEntity.ok(session);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String id) {
        Map<String, Object> session = sessionWorkflowService.getSession(id);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    @GetMapping("/{id}/memory-reinforcement")
    public ResponseEntity<Map<String, Object>> getMemoryReinforcement(@PathVariable String id) {
        try {
            return ResponseEntity.ok(sessionWorkflowService.getMemoryReinforcementStatus(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listSessions() {
        return ResponseEntity.ok(sessionWorkflowService.listSessions());
    }

    @GetMapping("/{id}/stream")
    public SseEmitter streamSession(@PathVariable String id) {
        SseEmitter emitter = new SseEmitter(10_000L);
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> session = sessionWorkflowService.getSession(id);
                emitter.send(SseEmitter.event().name("session-status").data(session == null ? Map.of("sessionId", id, "status", "UNKNOWN") : session));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }
}
