package com.aipa.workflow.confidence;

import java.util.List;
import java.util.Map;

/**
 * ConfidenceEngine — 信心評估引擎介面
 */
public interface ConfidenceEngine {
    ConfidenceScore evaluate(String requirement, List<Map<String, Object>> knowledgeRefs,
                            Map<String, Object> memoryContext, int threshold);
    boolean canProceed(ConfidenceScore score, int threshold);
}

