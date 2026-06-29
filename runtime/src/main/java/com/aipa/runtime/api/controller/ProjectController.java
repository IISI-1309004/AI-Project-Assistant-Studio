package com.aipa.runtime.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/project")
public class ProjectController {

    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initProject(@RequestBody Map<String, String> body) {
        String jobId = "job-" + UUID.randomUUID().toString().substring(0, 8);
        return ResponseEntity.accepted().body(Map.of(
                "jobId", jobId,
                "status", "STARTED",
                "projectRoot", body.getOrDefault("projectRoot", ""),
                "message", "Project init started (Phase 1 skeleton — scanner not yet implemented)"
        ));
    }

    @GetMapping("/init/{jobId}/status")
    public ResponseEntity<Map<String, Object>> getInitStatus(@PathVariable String jobId) {
        return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "status", "COMPLETED",
                "progress", 100,
                "message", "Phase 1 skeleton"
        ));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listProjects() {
        return ResponseEntity.ok(List.of());
    }
}
