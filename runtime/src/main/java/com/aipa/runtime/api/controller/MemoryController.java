package com.aipa.runtime.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/memory")
public class MemoryController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listMemory(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String projectId) {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMemory(@PathVariable String id) {
        return ResponseEntity.notFound().build();
    }
}
