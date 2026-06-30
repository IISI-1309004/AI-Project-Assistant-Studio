package com.aipa.workflow.spec;

import java.util.List;
import java.util.Map;

public record SpecRequest(
        String projectId,
        String projectRoot,
        String sessionId,
        String rawRequirement,
        SpecType type,
        List<Map<String, Object>> knowledgeRefs,
        Map<String, Object> memoryContext,
        int initialConfidenceScore
) {
}

