package com.aipa.runtime.api.controller;

import com.aipa.runtime.context.ProjectContextHolder;
import com.aipa.runtime.domain.Project;
import com.aipa.runtime.service.ProjectManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ProjectManagementController — 項目多租戶 REST API
 *
 * 提供的端點：
 * - POST   /api/v1/projects              # 創建新項目
 * - GET    /api/v1/projects              # 列出所有項目
 * - GET    /api/v1/projects/{projectId}  # 查詢單個項目
 * - PUT    /api/v1/projects/{projectId}  # 更新項目信息
 * - PATCH  /api/v1/projects/{projectId}/activate    # 激活項目
 * - PATCH  /api/v1/projects/{projectId}/suspend     # 暫停項目
 * - PATCH  /api/v1/projects/{projectId}/resume      # 恢復項目
 * - PATCH  /api/v1/projects/{projectId}/archive     # 存檔項目
 * - DELETE /api/v1/projects/{projectId}  # 刪除項目（謹慎）
 */
@RestController
@RequestMapping("/api/v1")
public class ProjectManagementController {

    private final ProjectManagementService projectManagementService;
    private final ProjectContextHolder contextHolder;

    public ProjectManagementController(
            ProjectManagementService projectManagementService,
            ProjectContextHolder contextHolder
    ) {
        this.projectManagementService = projectManagementService;
        this.contextHolder = contextHolder;
    }

    /**
     * 創建新項目
     *
     * POST /api/v1/projects
     * {
     *   "name": "customer-service",
     *   "rootPath": "/path/to/customer-service",
     *   "description": "Customer Service Module"
     * }
     */
    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> createProject(
            @RequestBody CreateProjectRequest request
    ) {
        Project project = projectManagementService.createProject(
            request.name(),
            request.rootPath(),
            request.description()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ProjectResponse.from(project));
    }

    /**
     * 列出所有項目
     *
     * GET /api/v1/projects[?status=ACTIVE][&owner=userId]
     */
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponse>> listProjects(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String owner
    ) {
        List<Project> projects;

        if ("ACTIVE".equalsIgnoreCase(status)) {
            projects = projectManagementService.getActiveProjects();
        } else if (owner != null && !owner.isBlank()) {
            projects = projectManagementService.getProjectsByOwner(owner);
        } else {
            projects = projectManagementService.getAllProjects();
        }

        List<ProjectResponse> responses = projects.stream()
            .map(ProjectResponse::from)
            .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * 查詢單個項目
     *
     * GET /api/v1/projects/{projectId}
     */
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable String projectId
    ) {
        Project project = projectManagementService.getProjectById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        return ResponseEntity.ok(ProjectResponse.from(project));
    }

    /**
     * 更新項目
     *
     * PUT /api/v1/projects/{projectId}
     * {
     *   "name": "new-name",
     *   "description": "new description"
     * }
     */
    @PutMapping("/projects/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable String projectId,
            @RequestBody UpdateProjectRequest request
    ) {
        Project project = projectManagementService.updateProject(
            projectId,
            request.name(),
            request.description()
        );

        return ResponseEntity.ok(ProjectResponse.from(project));
    }

    /**
     * 激活項目
     *
     * PATCH /api/v1/projects/{projectId}/activate
     */
    @PatchMapping("/projects/{projectId}/activate")
    public ResponseEntity<ProjectResponse> activateProject(
            @PathVariable String projectId
    ) {
        Project project = projectManagementService.markProjectAsActive(projectId);
        return ResponseEntity.ok(ProjectResponse.from(project));
    }

    /**
     * 暫停項目
     *
     * PATCH /api/v1/projects/{projectId}/suspend
     */
    @PatchMapping("/projects/{projectId}/suspend")
    public ResponseEntity<ProjectResponse> suspendProject(
            @PathVariable String projectId
    ) {
        Project project = projectManagementService.suspendProject(projectId);
        return ResponseEntity.ok(ProjectResponse.from(project));
    }

    /**
     * 恢復項目
     *
     * PATCH /api/v1/projects/{projectId}/resume
     */
    @PatchMapping("/projects/{projectId}/resume")
    public ResponseEntity<ProjectResponse> resumeProject(
            @PathVariable String projectId
    ) {
        Project project = projectManagementService.resumeProject(projectId);
        return ResponseEntity.ok(ProjectResponse.from(project));
    }

    /**
     * 存檔項目
     *
     * PATCH /api/v1/projects/{projectId}/archive
     */
    @PatchMapping("/projects/{projectId}/archive")
    public ResponseEntity<ProjectResponse> archiveProject(
            @PathVariable String projectId
    ) {
        Project project = projectManagementService.archiveProject(projectId);
        return ResponseEntity.ok(ProjectResponse.from(project));
    }

    /**
     * 獲取當前請求的項目上下文
     *
     * GET /api/v1/projects/context/current
     */
    @GetMapping("/projects/context/current")
    public ResponseEntity<Map<String, String>> getCurrentProjectContext() {
        if (!contextHolder.hasProjectId()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(Map.of(
            "projectId", contextHolder.getProjectId(),
            "operationId", contextHolder.getOperationId() != null ? contextHolder.getOperationId() : ""
        ));
    }

    // ===== Request/Response DTOs =====

    public record CreateProjectRequest(
        String name,
        String rootPath,
        String description
    ) {}

    public record UpdateProjectRequest(
        String name,
        String description
    ) {}

    public record ProjectResponse(
        String id,
        String name,
        String rootPath,
        String status,
        String description,
        String ownerId,
        long createdAt,
        long lastScanAt
    ) {
        public static ProjectResponse from(Project project) {
            return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getRootPath(),
                project.getStatus().getCode(),
                project.getDescription(),
                project.getOwnerId(),
                project.getCreatedAt() != null ? project.getCreatedAt().toEpochMilli() : 0,
                project.getLastScanAt() != null ? project.getLastScanAt().toEpochMilli() : 0
            );
        }
    }
}

