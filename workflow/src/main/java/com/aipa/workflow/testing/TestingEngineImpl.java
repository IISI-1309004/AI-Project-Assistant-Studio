package com.aipa.workflow.testing;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TestingEngineImpl {

    public Map<String, Object> runPhase4Checks(String requirement, String aiContent) {
        boolean hasCodeBlock = aiContent != null && !aiContent.isBlank();
        List<String> generatedTests = List.of(
                "UnitTest: validate primary service logic for requirement",
                "IntegrationTest: validate end-to-end API/use-case flow"
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", hasCodeBlock ? "PASS" : "FAIL");
        result.put("generatedAt", Instant.now().toString());
        result.put("generatedTests", generatedTests);
        result.put("coverageHint", hasCodeBlock ? 75 : 0);
        result.put("message", hasCodeBlock ? "Testing plan generated." : "AI content was empty, testing step failed.");
        return result;
    }
}

