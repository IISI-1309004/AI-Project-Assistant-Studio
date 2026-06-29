package com.aipa.workflow.review;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/**
 * ReviewEngineImpl — Phase 1 骨架（Phase 4 實作審查邏輯）
 */
@Service
public class ReviewEngineImpl {

    public Map<String, Object> review(List<String> changedFiles) {
        // TODO Phase 4：實作 ArchitectureReviewer、SecurityReviewer 等多維度審查
        return Map.of(
                "status", "PASS",
                "findings", List.of(),
                "message", "Phase 1 skeleton — Review Engine not yet implemented"
        );
    }

    public boolean canCreatePR(Map<String, Object> reviewResult) {
        // TODO Phase 4：判斷是否有 FAIL / BLOCK 級別結果
        return true;
    }
}
