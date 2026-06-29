package com.aipa.workflow.planning;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/**
 * PlanningEngineImpl — Phase 1 骨架（Phase 3 實作任務分解）
 */
@Service
public class PlanningEngineImpl {

    public Map<String, Object> createTaskPlan(String specId) {
        // TODO Phase 3：實作 TaskDecomposer、DAGValidator
        return Map.of(
                "planId", java.util.UUID.randomUUID().toString(),
                "tasks", List.of(),
                "message", "Phase 1 skeleton — Planning Engine not yet implemented"
        );
    }
}
