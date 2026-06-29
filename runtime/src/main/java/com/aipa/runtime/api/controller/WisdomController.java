package com.aipa.runtime.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wisdom")
public class WisdomController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listRules(
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addRule(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "id", UUID.randomUUID().toString(),
                "message", "Wisdom rule created (Phase 1 skeleton)"
        ));
    }
}
