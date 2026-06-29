package com.aipa.runtime.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listKnowledge(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addKnowledge(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "id", UUID.randomUUID().toString(),
                "message", "Knowledge item created (Phase 1 skeleton)"
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchKnowledge(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchKnowledgePost(
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(List.of());
    }
}
