package com.aipa.runtime.api.controller;

import com.aipa.runtime.service.WisdomEngineClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * WisdomController — Phase 6
 * 代理至 AI Engine Wisdom API，提供智慧規則管理端點
 */
@RestController
@RequestMapping("/api/v1/wisdom")
public class WisdomController {

    private final WisdomEngineClient wisdomEngineClient;

    public WisdomController(WisdomEngineClient wisdomEngineClient) {
        this.wisdomEngineClient = wisdomEngineClient;
    }

    @GetMapping("/rules")
    public ResponseEntity<List<Map<String, Object>>> listRules(
            @RequestParam(defaultValue = "") String projectId) {
        return ResponseEntity.ok(wisdomEngineClient.listRules(projectId));
    }

    @PostMapping("/rules")
    public ResponseEntity<Map<String, Object>> addRule(@RequestBody Map<String, Object> rule) {
        return ResponseEntity.ok(wisdomEngineClient.addRule(rule));
    }

    @PostMapping("/match")
    public ResponseEntity<List<Map<String, Object>>> matchRules(@RequestBody Map<String, Object> context) {
        return ResponseEntity.ok(wisdomEngineClient.matchRules(context));
    }

    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkForBlockViolations(@RequestBody Map<String, Object> context) {
        List<Map<String, Object>> matched = wisdomEngineClient.matchRules(context);
        boolean hasBlock = matched.stream().anyMatch(r -> "BLOCK".equals(r.get("severity")));
        long blockCount = matched.stream().filter(r -> "BLOCK".equals(r.get("severity"))).count();
        long warnCount = matched.stream().filter(r -> "WARN".equals(r.get("severity"))).count();
        return ResponseEntity.ok(Map.of(
                "hasBlockViolation", hasBlock,
                "blockCount", blockCount,
                "warnCount", warnCount,
                "matchedRules", matched
        ));
    }
}
