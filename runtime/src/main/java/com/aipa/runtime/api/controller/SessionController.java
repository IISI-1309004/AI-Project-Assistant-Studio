package com.aipa.runtime.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/session")
public class SessionController {

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(@RequestBody Map<String, String> body) {
        // Phase 1 骨架：回傳模擬 Session ID
        String sessionId = "s-" + UUID.randomUUID().toString().substring(0, 8);
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "status", "CREATED",
                "requirement", body.getOrDefault("requirement", ""),
                "message", "Session created (Phase 1 skeleton — workflow not yet implemented)"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String id) {
        return ResponseEntity.ok(Map.of(
                "sessionId", id,
                "status", "CREATED",
                "message", "Session query (Phase 1 skeleton)"
        ));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listSessions() {
        return ResponseEntity.ok(List.of());
    }
}
