package com.aipa.runtime.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SessionCompletionReportService — 會話完成報告服務
 * Phase 5-2: 生成完成報告並觸發自動學習流程
 */
@Service
public class SessionCompletionReportService {

    private final LearningEngineClient learningEngineClient;
    private final ObjectMapper objectMapper;
    private final boolean autoTriggerOnCompletion;
    private final boolean generateCompletionReport;

    public SessionCompletionReportService(
            LearningEngineClient learningEngineClient,
            ObjectMapper objectMapper,
            @Value("${aipa.learning.auto-trigger-on-completion:true}") boolean autoTriggerOnCompletion,
            @Value("${aipa.learning.generate-completion-report:true}") boolean generateCompletionReport
    ) {
        this.learningEngineClient = learningEngineClient;
        this.objectMapper = objectMapper;
        this.autoTriggerOnCompletion = autoTriggerOnCompletion;
        this.generateCompletionReport = generateCompletionReport;
    }

    /**
     * 生成會話完成報告
     */
    public SessionCompletionReport buildCompletionReport(Map<String, Object> session) {
        String sessionId = String.valueOf(session.getOrDefault("sessionId", "unknown"));
        String projectId = String.valueOf(session.getOrDefault("projectId", "unknown"));
        String requirement = String.valueOf(session.getOrDefault("requirement", ""));
        int confidenceScore = Integer.parseInt(String.valueOf(session.getOrDefault("confidenceScore", 0)));

        // 從 spec 提取信息
        String specTitle = "Unknown Spec";
        Map<String, Object> spec = objectMapper.convertValue(session.get("spec"), Map.class);
        if (spec != null) {
            specTitle = String.valueOf(spec.getOrDefault("title", "Unknown Spec"));
        }

        // 提取知識主題
        List<String> knowledgeTopics = extractKnowledgeTopics(session);

        // 提取關鍵學習
        List<String> keyLearnings = extractKeyLearnings(session);

        // 獲取執行狀態
        Map<String, Object> execution = objectMapper.convertValue(session.getOrDefault("execution", Map.of()), Map.class);
        String executionStatus = String.valueOf(execution.getOrDefault("status", "UNKNOWN"));

        // 獲取 PR Preview
        Map<String, Object> prPreview = objectMapper.convertValue(execution.getOrDefault("prPreview", Map.of()), Map.class);
        String prTitle = String.valueOf(prPreview.getOrDefault("title", ""));

        int attempts = Integer.parseInt(String.valueOf(session.getOrDefault("executionAttempts", 1)));

        return new SessionCompletionReport(
                sessionId, projectId, requirement, specTitle, confidenceScore,
                knowledgeTopics, keyLearnings, executionStatus, prTitle, attempts,
                Instant.now()
        );
    }

    /**
     * 觸發自動學習
     */
    public Map<String, Object> triggerAutoLearning(Map<String, Object> session, SessionCompletionReport report) {
        if (!autoTriggerOnCompletion) {
            return Map.of("message", "Auto-learning is disabled");
        }

        try {
            Map<String, Object> learningPayload = Map.of(
                    "project_id", report.projectId(),
                    "session_id", report.sessionId(),
                    "summary", generateLearningsSummary(report),
                    "changed_files", extractChangedFiles(session),
                    "review_comments", report.keyLearnings(),
                    "confidence_score", report.confidenceScore(),
                    "execution_status", report.executionStatus()
            );

            Map<String, Object> result = learningEngineClient.analyze(learningPayload);
            if (result != null) {
                // 記錄學習 ID 到 session
                session.put("learningId", result.get("learning_id"));
                session.put("learningTriggeredAt", Instant.now().toString());
                session.put("learningStatus", result.get("status"));
                return result;
            }
        } catch (Exception ex) {
            // 學習失敗，但不影響會話完成
            return Map.of("error", "Failed to trigger learning", "message", ex.getMessage());
        }

        return Map.of("message", "Learning trigger completed");
    }

    /**
     * 生成學習摘要文字
     */
    private String generateLearningsSummary(SessionCompletionReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Completed: ").append(report.specTitle()).append("\n");
        sb.append("Status: ").append(report.executionStatus()).append("\n");
        sb.append("Confidence: ").append(report.confidenceScore()).append("/100\n");
        if (!report.keyLearnings().isEmpty()) {
            sb.append("Key Learnings:\n");
            for (String learning : report.keyLearnings()) {
                sb.append("- ").append(learning).append("\n");
            }
        }
        return sb.toString();
    }

    private List<String> extractKnowledgeTopics(Map<String, Object> session) {
        List<String> topics = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> knowledgeRefs = (List<Map<String, Object>>) session.getOrDefault("knowledgeRefs", List.of());
        for (Map<String, Object> ref : knowledgeRefs) {
            String title = String.valueOf(ref.getOrDefault("title", ""));
            if (!title.isEmpty()) {
                topics.add(title);
            }
        }
        return topics;
    }

    private List<String> extractKeyLearnings(Map<String, Object> session) {
        List<String> learnings = new ArrayList<>();
        // 從 spec 和 execution 中提取關鍵信息
        Map<String, Object> spec = objectMapper.convertValue(session.get("spec"), Map.class);
        if (spec != null) {
            String title = String.valueOf(spec.getOrDefault("title", ""));
            if (!title.isEmpty()) {
                learnings.add("Completed implementation of: " + title);
            }
        }
        Map<String, Object> execution = objectMapper.convertValue(session.getOrDefault("execution", Map.of()), Map.class);
        if (!execution.isEmpty()) {
            String status = String.valueOf(execution.getOrDefault("status", ""));
            if ("PR_READY".equals(status)) {
                learnings.add("AI successfully generated and reviewed PR");
            }
        }
        return learnings;
    }

    private List<String> extractChangedFiles(Map<String, Object> session) {
        List<String> files = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Map<String, Object> execution = (Map<String, Object>) session.getOrDefault("execution", Map.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> ai = (Map<String, Object>) execution.getOrDefault("ai", Map.of());
        @SuppressWarnings("unchecked")
        List<String> generatedFiles = (List<String>) ai.getOrDefault("generatedFiles", List.of());
        return generatedFiles;
    }

    public boolean isAutoLearningEnabled() {
        return autoTriggerOnCompletion;
    }

    public boolean isCompletionReportEnabled() {
        return generateCompletionReport;
    }
}

