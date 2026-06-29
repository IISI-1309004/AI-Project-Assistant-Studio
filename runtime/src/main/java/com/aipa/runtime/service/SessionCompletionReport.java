package com.aipa.runtime.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SessionCompletionReport — 會話完成報告
 * Phase 5-2: 生成自動學習的摘要報告，包含知識庫和記憶摘要
 */
public class SessionCompletionReport {
    private final String sessionId;
    private final String projectId;
    private final String requirement;
    private final String specTitle;
    private final int confidenceScore;
    private final List<String> knowledgeTopics;
    private final List<String> keyLearnings;
    private final String executionStatus;
    private final String prPreview;
    private final int attempts;
    private final Instant completedAt;

    public SessionCompletionReport(
            String sessionId, String projectId, String requirement, String specTitle,
            int confidenceScore, List<String> knowledgeTopics, List<String> keyLearnings,
            String executionStatus, String prPreview, int attempts, Instant completedAt) {
        this.sessionId = sessionId;
        this.projectId = projectId;
        this.requirement = requirement;
        this.specTitle = specTitle;
        this.confidenceScore = confidenceScore;
        this.knowledgeTopics = knowledgeTopics;
        this.keyLearnings = keyLearnings;
        this.executionStatus = executionStatus;
        this.prPreview = prPreview;
        this.attempts = attempts;
        this.completedAt = completedAt;
    }

    // Getters
    public String sessionId() { return sessionId; }
    public String projectId() { return projectId; }
    public String requirement() { return requirement; }
    public String specTitle() { return specTitle; }
    public int confidenceScore() { return confidenceScore; }
    public List<String> knowledgeTopics() { return knowledgeTopics; }
    public List<String> keyLearnings() { return keyLearnings; }
    public String executionStatus() { return executionStatus; }
    public String prPreview() { return prPreview; }
    public int attempts() { return attempts; }
    public Instant completedAt() { return completedAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sessionId", sessionId);
        map.put("projectId", projectId);
        map.put("requirement", requirement);
        map.put("specTitle", specTitle);
        map.put("confidenceScore", confidenceScore);
        map.put("knowledgeTopics", knowledgeTopics);
        map.put("keyLearnings", keyLearnings);
        map.put("executionStatus", executionStatus);
        map.put("prPreview", prPreview);
        map.put("attempts", attempts);
        map.put("completedAt", completedAt.toString());
        return map;
    }
}

