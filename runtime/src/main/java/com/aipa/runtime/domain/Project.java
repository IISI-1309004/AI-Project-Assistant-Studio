package com.aipa.runtime.domain;

import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Map;

/**
 * Project Entity — 代表一個由 AIPA Studio 管理的軟體項目
 *
 * 每個項目有獨立的知識庫、記憶庫、規則集和經驗庫。
 * 一對多架構允許一個 Runtime 服務多個項目。
 */
@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_project_root_path", columnList = "root_path", unique = true),
    @Index(name = "idx_project_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class Project {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT", nullable = false, unique = true)
    private String rootPath;

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant lastScanAt;

    @Column(columnDefinition = "TEXT")
    private String configJson;

    @Column(columnDefinition = "TEXT")
    private String dnaJson;

    @Column(columnDefinition = "TEXT")
    private String techStackJson;

    @Column(length = 36)
    private String ownerId;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ===== Constructors =====

    public Project() {
    }

    public Project(String id, String name, String rootPath) {
        this.id = id;
        this.name = name;
        this.rootPath = rootPath;
        this.status = ProjectStatus.INITIALIZING;
        this.createdAt = Instant.now();
    }

    // ===== Getters & Setters =====

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastScanAt() {
        return lastScanAt;
    }

    public void setLastScanAt(Instant lastScanAt) {
        this.lastScanAt = lastScanAt;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getDnaJson() {
        return dnaJson;
    }

    public void setDnaJson(String dnaJson) {
        this.dnaJson = dnaJson;
    }

    public String getTechStackJson() {
        return techStackJson;
    }

    public void setTechStackJson(String techStackJson) {
        this.techStackJson = techStackJson;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // ===== Business Logic =====

    public void markAsActive() {
        this.status = ProjectStatus.ACTIVE;
    }

    public void markAsScanned(Instant scanTime) {
        this.lastScanAt = scanTime;
    }

    public boolean isActive() {
        return status == ProjectStatus.ACTIVE;
    }

    @Override
    public String toString() {
        return "Project{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}

