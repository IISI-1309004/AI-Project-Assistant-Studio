package com.aipa.workflow.review;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ReviewEngineImpl — Phase 4 最小審查實作
 */
@Service
public class ReviewEngineImpl {

    public Map<String, Object> review(String aiContent, List<String> changedFiles) {
        List<Map<String, Object>> findings = new ArrayList<>();
        String normalized = aiContent == null ? "" : aiContent.toLowerCase();

        if (normalized.contains("select *") && !normalized.contains("where")) {
            findings.add(finding("SQL", "WARN", "Possible missing WHERE condition in generated SQL snippet"));
        }
        if (normalized.contains("jdbc") && !normalized.contains("transaction")) {
            findings.add(finding("SQL", "WARN", "Generated persistence logic does not mention transaction handling"));
        }
        if (normalized.contains("password") || normalized.contains("secret")) {
            findings.add(finding("SECURITY", "FAIL", "Sensitive information appears in generated content"));
        }
        if (normalized.contains("circular-dependency") || normalized.contains("cycle")) {
            findings.add(finding("ARCHITECTURE", "FAIL", "Generated plan hints at a cyclic dependency"));
        }

        String status = findings.stream().anyMatch(item -> "FAIL".equals(item.get("severity"))) ? "FAIL" : "PASS";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", status);
        result.put("generatedAt", Instant.now().toString());
        result.put("changedFiles", changedFiles);
        result.put("findings", findings);
        result.put("message", findings.isEmpty() ? "Review checks passed." : "Review completed with findings.");
        return result;
    }

    public boolean canCreatePR(Map<String, Object> reviewResult) {
        return "PASS".equals(String.valueOf(reviewResult.get("status")));
    }

    private Map<String, Object> finding(String category, String severity, String message) {
        Map<String, Object> finding = new LinkedHashMap<>();
        finding.put("category", category);
        finding.put("severity", severity);
        finding.put("message", message);
        return finding;
    }
}
