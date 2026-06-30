package com.aipa.runtime.service;

import com.aipa.runtime.domain.Project;
import com.aipa.runtime.domain.ProjectStatus;
import com.aipa.runtime.persistence.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ProjectManagementService — 多專案管理服務
 *
 * 負責項目的生命週期管理：
 * - 項目初始化、狀態轉換
 * - 項目信息查詢和更新
 * - 項目清理和刪除
 */
@Service
public class ProjectManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectManagementService.class);

    private final ProjectRepository projectRepository;

    public ProjectManagementService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * 創建新項目
     */
    @Transactional
    public Project createProject(String name, String rootPath, String description) {
        // 驗證根路徑是否存在
        Path path = Paths.get(rootPath);
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Root path does not exist or is not a directory: " + rootPath);
        }

        // 檢查是否已存在相同根路徑的項目
        if (projectRepository.existsByRootPath(rootPath)) {
            throw new IllegalArgumentException("A project with this root path already exists");
        }

        // 生成項目 ID（使用根路徑目錄名或提供的名稱）
        String projectId = generateProjectId(name, rootPath);

        // 建立項目實體
        Project project = new Project(projectId, name, rootPath);
        project.setDescription(description);
        project.setStatus(ProjectStatus.INITIALIZING);

        Project saved = projectRepository.save(project);
        logger.info("Project created: {} (id={})", name, projectId);

        return saved;
    }

    /**
     * 根據 ID 查詢項目
     */
    public Optional<Project> getProjectById(String projectId) {
        return projectRepository.findById(projectId);
    }

    /**
     * 根據根路徑查詢項目
     */
    public Optional<Project> getProjectByRootPath(String rootPath) {
        return projectRepository.findByRootPath(rootPath);
    }

    /**
     * 查詢所有項目
     */
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * 查詢所有活躍項目
     */
    public List<Project> getActiveProjects() {
        return projectRepository.findByStatus(ProjectStatus.ACTIVE);
    }

    /**
     * 根據所有者查詢項目
     */
    public List<Project> getProjectsByOwner(String ownerId) {
        return projectRepository.findByOwnerId(ownerId);
    }

    /**
     * 模糊查詢項目
     */
    public List<Project> searchProjects(String namePart) {
        return projectRepository.findByNameContaining(namePart);
    }

    /**
     * 標記項目為活躍
     */
    @Transactional
    public Project markProjectAsActive(String projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        project.setStatus(ProjectStatus.ACTIVE);
        Project updated = projectRepository.save(project);
        logger.info("Project marked as active: {} (id={})", project.getName(), projectId);

        return updated;
    }

    /**
     * 暫停項目
     */
    @Transactional
    public Project suspendProject(String projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        project.setStatus(ProjectStatus.SUSPENDED);
        Project updated = projectRepository.save(project);
        logger.info("Project suspended: {} (id={})", project.getName(), projectId);

        return updated;
    }

    /**
     * 恢復已暫停的項目
     */
    @Transactional
    public Project resumeProject(String projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getStatus() != ProjectStatus.SUSPENDED) {
            throw new IllegalStateException("Only suspended projects can be resumed");
        }

        project.setStatus(ProjectStatus.ACTIVE);
        Project updated = projectRepository.save(project);
        logger.info("Project resumed: {} (id={})", project.getName(), projectId);

        return updated;
    }

    /**
     * 存檔項目
     */
    @Transactional
    public Project archiveProject(String projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        project.setStatus(ProjectStatus.ARCHIVED);
        Project updated = projectRepository.save(project);
        logger.info("Project archived: {} (id={})", project.getName(), projectId);

        return updated;
    }

    /**
     * 更新項目信息
     */
    @Transactional
    public Project updateProject(String projectId, String name, String description) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (name != null && !name.isBlank()) {
            project.setName(name);
        }
        if (description != null) {
            project.setDescription(description);
        }

        Project updated = projectRepository.save(project);
        logger.info("Project updated: {} (id={})", project.getName(), projectId);

        return updated;
    }

    /**
     * 記錄項目掃描時間
     */
    @Transactional
    public void recordProjectScan(String projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        project.setLastScanAt(Instant.now());
        projectRepository.save(project);
    }

    /**
     * 生成項目 ID
     */
    private String generateProjectId(String name, String rootPath) {
        String proposed;

        if (name != null && !name.isBlank()) {
            // 根據提供的名稱生成
            proposed = name.toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        } else {
            // 根據目錄名稱生成
            proposed = rootPath.replaceAll("\\\\", "/")
                .toLowerCase()
                .replaceAll(".*/", "")
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        }

        // 確保不為空
        if (proposed.isEmpty()) {
            proposed = "project-" + UUID.randomUUID().toString().substring(0, 8);
        }

        // 確保不超過 36 字符（UUID 長度）
        if (proposed.length() > 36) {
            proposed = proposed.substring(0, 36);
        }

        return proposed;
    }
}

