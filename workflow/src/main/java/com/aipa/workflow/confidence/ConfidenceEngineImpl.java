package com.aipa.workflow.confidence;

import org.springframework.stereotype.Service;

/**
 * ConfidenceEngineImpl — Phase 1 骨架（Phase 3 實作評估邏輯）
 */
@Service
public class ConfidenceEngineImpl {

    public ConfidenceScore evaluate(String specContent, int threshold) {
        // TODO Phase 3：實作 5 個維度評估（Knowledge/Memory/Experience/Architecture/Business）
        return ConfidenceScore.notImplemented();
    }

    public boolean canProceed(ConfidenceScore score, int threshold) {
        return score.isAboveThreshold(threshold);
    }
}
