package com.aipa.runtime.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/checkpoint")
public class CheckpointController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listPendingCheckpoints(
            @RequestParam(required = false) String sessionId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveCheckpoint(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.ok(Map.of(
                "checkpointId", id,
                "status", "APPROVED",
                "message", "Checkpoint approved (Phase 1 skeleton)"
        ));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectCheckpoint(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.ok(Map.of(
                "checkpointId", id,
                "status", "REJECTED",
                "message", "Checkpoint rejected (Phase 1 skeleton)"
        ));
    }
}
