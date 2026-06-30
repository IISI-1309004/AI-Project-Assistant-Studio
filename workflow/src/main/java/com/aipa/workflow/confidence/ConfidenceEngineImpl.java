package com.aipa.workflow.confidence;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ConfidenceEngineImpl — Phase 3 信心評估邏輯
 */
@Service
public class ConfidenceEngineImpl implements ConfidenceEngine {

    @Override
    public ConfidenceScore evaluate(String requirement, List<Map<String, Object>> knowledgeRefs,
                                    Map<String, Object> memoryContext, int threshold) {
        int knowledgeScore = Math.min(30, 15 + knowledgeRefs.size() * 6);
        int memoryScore = Math.min(20, 8 + countMemorySignals(memoryContext) * 4);
        int experienceScore = 0;
        int architectureScore = inferArchitectureScore(knowledgeRefs);
        int businessScore = inferBusinessScore(requirement);

        Map<String, Integer> dimensions = new LinkedHashMap<>();
        dimensions.put("knowledge", knowledgeScore);
        dimensions.put("memory", memoryScore);
        dimensions.put("experience", experienceScore);
        dimensions.put("architecture", architectureScore);
        dimensions.put("business", businessScore);

        int total = Math.min(100, knowledgeScore + memoryScore + experienceScore + architectureScore + businessScore);
        List<String> missingItems = new ArrayList<>();
        if (knowledgeRefs.isEmpty()) {
            missingItems.add("缺少相關知識項目，請先執行 aipa init 或補充更多專案上下文");
        }
        if (countMemorySignals(memoryContext) == 0) {
            missingItems.add("缺少 Memory Context（架構決策 / Coding Style / Business Rules）");
        }
        if (isHighRiskRequirement(requirement) && total < threshold + 10) {
            missingItems.add("需求涉及較高風險領域，建議補充業務規則與回滾策略細節");
        }

        String report = missingItems.isEmpty()
                ? "信心評估通過，可進入 Task Planning。"
                : "NMI：" + String.join("；", missingItems);
        return new ConfidenceScore(total, dimensions, missingItems, report);
    }

    @Override
    public boolean canProceed(ConfidenceScore score, int threshold) {
        return score.isAboveThreshold(threshold);
    }

    private int countMemorySignals(Map<String, Object> memoryContext) {
        if (memoryContext == null || memoryContext.isEmpty()) {
            return 0;
        }
        return (int) memoryContext.values().stream()
                .filter(List.class::isInstance)
                .map(List.class::cast)
                .filter(list -> !list.isEmpty())
                .count();
    }

    private int inferArchitectureScore(List<Map<String, Object>> knowledgeRefs) {
        boolean hasArchitecture = knowledgeRefs.stream()
                .anyMatch(item -> "ARCHITECTURE".equals(String.valueOf(item.get("category")))
                        || "PROJECT".equals(String.valueOf(item.get("category"))));
        return hasArchitecture ? 18 : 10;
    }

    private int inferBusinessScore(String requirement) {
        if (isHighRiskRequirement(requirement)) {
            return 12;
        }
        if (requirement != null && (requirement.contains("提醒") || requirement.contains("通知") || requirement.contains("查詢"))) {
            return 20;
        }
        return 16;
    }

    private boolean isHighRiskRequirement(String requirement) {
        if (requirement == null) {
            return false;
        }
        String normalized = requirement.toLowerCase();
        return normalized.contains("付款")
                || normalized.contains("支付")
                || normalized.contains("刪除")
                || normalized.contains("資料遷移")
                || normalized.contains("migration");
    }
}
