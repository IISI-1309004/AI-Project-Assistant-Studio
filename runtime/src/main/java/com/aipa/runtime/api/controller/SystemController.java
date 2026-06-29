package com.aipa.runtime.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SystemController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "version", "1.0.0-SNAPSHOT",
                "timestamp", Instant.now().toString(),
                "phase", "Phase 1 — Skeleton"
        ));
    }

    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> version() {
        return ResponseEntity.ok(Map.of(
                "version", "1.0.0-SNAPSHOT",
                "buildTime", Instant.now().toString()
        ));
    }
}
