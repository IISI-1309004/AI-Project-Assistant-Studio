package com.aipa.runtime.api.controller;

import com.aipa.runtime.service.MemoryEngineClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MemoryController — Phase 6 完整實作
 * 代理至 AI Engine Memory API
 */
@RestController
@RequestMapping("/api/v1/memory")
public class MemoryController {

    private final MemoryEngineClient memoryEngineClient;

    public MemoryController(MemoryEngineClient memoryEngineClient) {
        this.memoryEngineClient = memoryEngineClient;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listMemory(
            @RequestParam(required = false, defaultValue = "") String type,
            @RequestParam(required = false, defaultValue = "") String projectId) {
        return ResponseEntity.ok(memoryEngineClient.query(projectId, type));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMemory(@PathVariable String id) {
        Map<String, Object> item = memoryEngineClient.getItem(id);
        if (item == null || item.containsKey("error")) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    @PostMapping("/reinforce/{id}")
    public ResponseEntity<Map<String, Object>> reinforce(@PathVariable String id) {
        Map<String, Object> result = memoryEngineClient.reinforce(id);
        if (result == null || result.containsKey("error")) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }
}
