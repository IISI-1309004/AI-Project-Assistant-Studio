package com.aipa.workflow.planning;

import com.aipa.workflow.confidence.ConfidenceScore;
import com.aipa.workflow.spec.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PlanningEngineImpl — Phase 3 任務分解實作
 */
@Service
public class PlanningEngineImpl implements PlanningEngine {

    @Override
    public Map<String, Object> createTaskPlan(Specification spec, ConfidenceScore confidenceScore) {
        String planId = UUID.randomUUID().toString();
        List<Map<String, Object>> tasks = new ArrayList<>();
        tasks.add(task("TASK-1", "分析既有實作與影響點", "檢閱 Knowledge Context 與既有程式碼位置", List.of()));
        tasks.add(task("TASK-2", "更新核心業務邏輯", "實作需求所需的服務 / Domain / Use Case 變更", List.of("TASK-1")));
        tasks.add(task("TASK-3", "更新 API / 契約", "調整 Controller、DTO、API 規格或事件契約", List.of("TASK-2")));
        tasks.add(task("TASK-4", "更新資料持久層", "如有需要，調整 Repository、Mapper、SQL 或資料表操作", List.of("TASK-2")));
        tasks.add(task("TASK-5", "補齊單元測試", "為主要邏輯新增單元測試", List.of("TASK-2")));
        tasks.add(task("TASK-6", "補齊整合測試", "驗證 API / 資料流 / 模組互動", List.of("TASK-3", "TASK-4")));
        tasks.add(task("TASK-7", "整理交付與準備 AI Execution", "確認規格、任務、風險與回滾方案", List.of("TASK-5", "TASK-6")));

        return new LinkedHashMap<>(Map.of(
                "planId", planId,
                "specId", spec.id(),
                "generatedAt", Instant.now().toString(),
                "confidenceScore", confidenceScore.value(),
                "summary", "已將需求分解為 7 個任務，等待 Task Approval。",
                "tasks", tasks
        ));
    }

    private Map<String, Object> task(String id, String title, String description, List<String> dependencies) {
        return new LinkedHashMap<>(Map.of(
                "id", id,
                "title", title,
                "description", description,
                "status", "PENDING",
                "dependencies", dependencies
        ));
    }
}
