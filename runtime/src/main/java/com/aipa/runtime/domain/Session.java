package com.aipa.runtime.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Session Entity — 代表一次完整的 `aipa ask` 工作流程
 *
 * 每個 Session 必須關聯到一個特定的 Project，確保多租戶隔離。
 */
@Entity
@Table(name = "sessions", indexes = {
    @Index(name = "idx_session_project_status", columnList = "project_id, status"),
    @Index(name = "idx_session_created_at", columnList = "created_at DESC"),
    @Index(name = "idx_session_user_id", columnList = "user_id")
})
public class Session {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(length = 36, nullable = false)
    private String projectId;

    @Column(length = 36)
    private String userId;

    @Column(length = 50, nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String requirement;

    @Column(length = 36)
    private String specId;

    @Column(length = 36)
    private String taskPlanId;

    @Column(columnDefinition = "TEXT")
    private String prUrl;

    @Column(columnDefinition = "TEXT")
    private String learningJson;

    @Column(columnDefinition = "TEXT")
    private String checkpointsJson;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant completedAt;

    @Column
    private Instant updatedAt;

    // ===== Constructors =====

    public Session() {
    }

    public Session(String id, String projectId, String requirement) {
        this.id = id;
        this.projectId = projectId;
        this.requirement = requirement;
        this.status = "CREATED";
        this.createdAt = Instant.now();
    }

    // ===== Getters & Setters =====

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }

    public String getSpecId() {
        return specId;
    }

    public void setSpecId(String specId) {
        this.specId = specId;
    }

    public String getTaskPlanId() {
        return taskPlanId;
    }

    public void setTaskPlanId(String taskPlanId) {
        this.taskPlanId = taskPlanId;
    }

    public String getPrUrl() {
        return prUrl;
    }

    public void setPrUrl(String prUrl) {
        this.prUrl = prUrl;
    }

    public String getLearningJson() {
        return learningJson;
    }

    public void setLearningJson(String learningJson) {
        this.learningJson = learningJson;
    }

    public String getCheckpointsJson() {
        return checkpointsJson;
    }

    public void setCheckpointsJson(String checkpointsJson) {
        this.checkpointsJson = checkpointsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Session{" +
                "id='" + id + '\'' +
                ", projectId='" + projectId + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

