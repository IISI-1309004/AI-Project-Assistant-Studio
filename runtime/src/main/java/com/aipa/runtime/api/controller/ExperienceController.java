package com.aipa.runtime.api.controller;

import com.aipa.runtime.service.ExperienceEngineClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ExperienceController — Phase 6
 * 代理至 AI Engine Experience API，提供相似案例搜尋端點
 */
@RestController
@RequestMapping("/api/v1/experience")
public class ExperienceController {

    private final ExperienceEngineClient experienceEngineClient;

    public ExperienceController(ExperienceEngineClient experienceEngineClient) {
        this.experienceEngineClient = experienceEngineClient;
    }

    @PostMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchSimilar(@RequestBody Map<String, Object> body) {
        String query = String.valueOf(body.getOrDefault("query", ""));
        String projectId = String.valueOf(body.getOrDefault("project_id", ""));
        int topK = body.get("top_k") instanceof Number n ? n.intValue() : 5;
        if (query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(experienceEngineClient.searchSimilar(query, projectId, topK));
    }

    @GetMapping("/cases")
    public ResponseEntity<List<Map<String, Object>>> listCases(
            @RequestParam(defaultValue = "") String projectId) {
        return ResponseEntity.ok(experienceEngineClient.listCases(projectId));
    }

    @PostMapping("/cases")
    public ResponseEntity<Map<String, Object>> createCase(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = experienceEngineClient.createCase(body);
        if (result.containsKey("error")) {
            return ResponseEntity.internalServerError().body(result);
        }
        return ResponseEntity.ok(result);
    }
}

