package com.aipa.runtime.service;

import com.aipa.workflow.confidence.ConfidenceEngine;
import com.aipa.workflow.confidence.ConfidenceScore;
import com.aipa.workflow.planning.PlanningEngine;
import com.aipa.workflow.spec.SpecEngine;
import com.aipa.workflow.spec.SpecRequest;
import com.aipa.workflow.spec.SpecType;
import com.aipa.workflow.spec.Specification;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@Service
public class SessionWorkflowService {

    private final KnowledgeEngineClient knowledgeEngineClient;
    private final MemoryEngineClient memoryEngineClient;
    private final SpecEngine specEngine;
    private final ConfidenceEngine confidenceEngine;
    private final PlanningEngine planningEngine;
    private final ExecutionPipelineService executionPipelineService;
    private final SessionCompletionReportService completionReportService;
    private final ObjectMapper objectMapper;
    private final int confidenceThreshold;
    private final int executionMaxRetries;
    private final boolean autoReinforceMemory;
    private final int reinforceMaxItems;
    private final Path stateRoot;
    private final ConcurrentMap<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Map<String, Object>> checkpoints = new ConcurrentHashMap<>();

    public SessionWorkflowService(
            KnowledgeEngineClient knowledgeEngineClient,
            MemoryEngineClient memoryEngineClient,
            SpecEngine specEngine,
            ConfidenceEngine confidenceEngine,
            PlanningEngine planningEngine,
            ExecutionPipelineService executionPipelineService,
            SessionCompletionReportService completionReportService,
            ObjectMapper objectMapper,
            @Value("${aipa.confidence-threshold:70}") int confidenceThreshold,
            @Value("${aipa.execution-max-retries:3}") int executionMaxRetries,
            @Value("${aipa.learning.auto-reinforce-memory:true}") boolean autoReinforceMemory,
            @Value("${aipa.learning.reinforce-max-items:3}") int reinforceMaxItems,
            @Value("${aipa.runtime-state-dir:.ai-project/runtime-state}") String runtimeStateDir
    ) {
        this.knowledgeEngineClient = knowledgeEngineClient;
        this.memoryEngineClient = memoryEngineClient;
        this.specEngine = specEngine;
        this.confidenceEngine = confidenceEngine;
        this.planningEngine = planningEngine;
        this.executionPipelineService = executionPipelineService;
        this.completionReportService = completionReportService;
        this.objectMapper = objectMapper.findAndRegisterModules();
        this.confidenceThreshold = confidenceThreshold;
        this.executionMaxRetries = Math.max(1, executionMaxRetries);
        this.autoReinforceMemory = autoReinforceMemory;
        this.reinforceMaxItems = Math.max(1, reinforceMaxItems);
        this.stateRoot = Path.of(runtimeStateDir);
        prepareStateDirectories();
    }

    public Map<String, Object> createSession(String projectId, String projectRoot, String requirement) {
        String sessionId = "s-" + UUID.randomUUID().toString().substring(0, 8);
        Path resolvedProjectRoot = Path.of(projectRoot == null || projectRoot.isBlank() ? "." : projectRoot).toAbsolutePath().normalize();
        prepareProjectWorkspace(resolvedProjectRoot);

        List<Map<String, Object>> knowledgeRefs = safeKnowledgeSearch(projectId, requirement);
        Map<String, Object> memoryContext = safeMemoryContext(projectId);
        ConfidenceScore preliminaryScore = confidenceEngine.evaluate(requirement, knowledgeRefs, memoryContext, confidenceThreshold);
        Specification spec = specEngine.generateSpec(new SpecRequest(
                projectId,
                resolvedProjectRoot.toString(),
                sessionId,
                requirement,
                SpecType.FEATURE,
                knowledgeRefs,
                memoryContext,
                preliminaryScore.value()
        ));

        String checkpointId = newId("cp-");
        Map<String, Object> checkpoint = checkpointMap(
                checkpointId,
                sessionId,
                "SPEC_APPROVAL",
                "PENDING",
                Map.of(
                        "specId", spec.id(),
                        "title", spec.title(),
                        "confidenceScore", preliminaryScore.value(),
                        "preview", summarize(spec.content())
                )
        );

        Map<String, Object> session = new LinkedHashMap<>();
        session.put("sessionId", sessionId);
        session.put("projectId", projectId);
        session.put("projectRoot", resolvedProjectRoot.toString());
        session.put("requirement", requirement);
        session.put("status", "SPEC_PENDING");
        session.put("specId", spec.id());
        session.put("taskPlanId", null);
        session.put("currentCheckpointId", checkpointId);
        session.put("confidenceScore", preliminaryScore.value());
        session.put("confidenceBreakdown", preliminaryScore.dimensions());
        session.put("nmiReport", "");
        session.put("message", "Specification generated. Awaiting Spec Approval.");
        session.put("createdAt", Instant.now().toString());
        session.put("updatedAt", Instant.now().toString());
        session.put("knowledgeRefs", knowledgeRefs);
        session.put("memoryContext", memoryContext);
        session.put("spec", spec);
        session.put("taskPlan", null);
        session.put("phase4Message", null);

        saveCheckpoint(checkpoint);
        saveSession(session);
        return session;
    }

    public Map<String, Object> getSession(String sessionId) {
        Map<String, Object> inMemory = sessions.get(sessionId);
        if (inMemory != null) {
            return inMemory;
        }
        Path file = stateRoot.resolve("sessions").resolve(sessionId + ".json");
        if (!Files.exists(file)) {
            return null;
        }
        try {
            Map<String, Object> session = objectMapper.readValue(file.toFile(), new TypeReference<>() { });
            sessions.put(sessionId, session);
            return session;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load session " + sessionId, ex);
        }
    }

    public List<Map<String, Object>> listSessions() {
        List<Map<String, Object>> results = new ArrayList<>();
        Path dir = stateRoot.resolve("sessions");
        if (!Files.exists(dir)) {
            return results;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try {
                    Map<String, Object> session = objectMapper.readValue(path.toFile(), new TypeReference<>() { });
                    sessions.put(String.valueOf(session.get("sessionId")), session);
                    results.add(session);
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to load session file: " + path, ex);
                }
            });
            return results.stream()
                    .sorted(Comparator.comparing((Map<String, Object> map) -> String.valueOf(map.get("createdAt"))).reversed())
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to list sessions", ex);
        }
    }

    public List<Map<String, Object>> listPendingCheckpoints(String sessionId) {
        Path dir = stateRoot.resolve("checkpoints");
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
             return files.filter(path -> path.toString().endsWith(".json"))
                    .map(this::loadCheckpoint)
                    .filter(checkpoint -> "PENDING".equals(String.valueOf(checkpoint.get("status"))))
                    .filter(checkpoint -> sessionId == null || sessionId.isBlank()
                            || sessionId.equals(String.valueOf(checkpoint.get("sessionId"))))
                    .sorted(Comparator.comparing((Map<String, Object> cp) -> String.valueOf(cp.get("triggeredAt"))))
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to list checkpoints", ex);
        }
    }

    public Map<String, Object> approveCheckpoint(String checkpointId, String actor, String comments) {
        Map<String, Object> checkpoint = requireCheckpoint(checkpointId);
        Map<String, Object> session = requireSession(String.valueOf(checkpoint.get("sessionId")));

        checkpoint.put("status", "APPROVED");
        checkpoint.put("resolvedAt", Instant.now().toString());
        checkpoint.put("resolvedBy", actor == null || actor.isBlank() ? "cli" : actor);
        checkpoint.put("comments", comments == null ? "" : comments);
        saveCheckpoint(checkpoint);
        appendAudit(session, checkpoint, "APPROVED", comments);

        String type = String.valueOf(checkpoint.get("type"));
        if ("SPEC_APPROVAL".equals(type)) {
            return advanceAfterSpecApproval(session, checkpoint);
        }
        if ("TASK_APPROVAL".equals(type)) {
            session.put("status", "EXECUTING");
            session.put("currentCheckpointId", null);
            session.put("message", "Task approved. Executing AI pipeline...");
            session.put("updatedAt", Instant.now().toString());
            executeAfterTaskApproval(session);
            saveSession(session);
        }
        if ("PR_APPROVAL".equals(type)) {
             session.put("status", "COMPLETED");
             session.put("message", "PR approved. Session completed.");
             session.put("updatedAt", Instant.now().toString());

             // Phase 5-2: 生成完成報告並觸發自動學習
             completeSessionWithLearning(session);
             saveSession(session);
         }
        return Map.of("checkpoint", checkpoint, "session", session);
    }

    private void executeAfterTaskApproval(Map<String, Object> session) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> knowledgeRefs = (List<Map<String, Object>>) session.getOrDefault("knowledgeRefs", List.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> memoryContext = (Map<String, Object>) session.getOrDefault("memoryContext", Map.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> spec = (Map<String, Object>) objectMapper.convertValue(session.get("spec"), new TypeReference<Map<String, Object>>() { });

        Map<String, Object> execution = Map.of();
        List<Map<String, Object>> executionHistory = new ArrayList<>();
        int attempts = 0;
        for (int i = 1; i <= executionMaxRetries; i++) {
            attempts = i;
            execution = executionPipelineService.execute(
                    String.valueOf(session.get("requirement")),
                    String.valueOf(spec.getOrDefault("content", "")),
                    knowledgeRefs,
                    memoryContext,
                    String.valueOf(session.get("sessionId"))
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> ai = (Map<String, Object>) execution.getOrDefault("ai", Map.of());
            executionHistory.add(Map.of(
                    "attempt", i,
                    "status", String.valueOf(execution.getOrDefault("status", "UNKNOWN")),
                    "provider", String.valueOf(ai.getOrDefault("provider", "unknown")),
                    "timestamp", Instant.now().toString()
            ));
            if ("PR_READY".equals(String.valueOf(execution.get("status")))) {
                break;
            }
        }

        session.put("execution", execution);
        session.put("executionAttempts", attempts);
        session.put("executionHistory", executionHistory);
        appendAiAudit(session, execution);
        session.put("phase4Message", "AI pipeline executed.");
        String executionStatus = String.valueOf(execution.get("status"));
        session.put("status", executionStatus);
        session.put("message", switch (executionStatus) {
            case "PR_READY" -> "AI execution, testing, and review completed. Awaiting PR Approval.";
            case "REVIEW_FAILED" -> "AI execution completed but review reported blocking issues after " + attempts + " attempts.";
            case "TEST_FAILED" -> "AI execution completed but testing step failed after " + attempts + " attempts.";
            default -> "AI execution failed after " + attempts + " attempts.";
        });
        if ("PR_READY".equals(executionStatus)) {
            String prCheckpointId = newId("cp-");
            Map<String, Object> prCheckpoint = checkpointMap(
                    prCheckpointId,
                    String.valueOf(session.get("sessionId")),
                    "PR_APPROVAL",
                    "PENDING",
                    Map.of(
                            "prPreview", execution.get("prPreview"),
                            "review", execution.get("review"),
                            "testing", execution.get("testing")
                    )
            );
            saveCheckpoint(prCheckpoint);
            session.put("status", "PR_PENDING");
            session.put("currentCheckpointId", prCheckpointId);
            session.put("message", "Execution completed. Awaiting PR Approval checkpoint.");
        }
        session.put("updatedAt", Instant.now().toString());
    }

    public Map<String, Object> rejectCheckpoint(String checkpointId, String actor, String comments) {
        Map<String, Object> checkpoint = requireCheckpoint(checkpointId);
        Map<String, Object> session = requireSession(String.valueOf(checkpoint.get("sessionId")));

        checkpoint.put("status", "REJECTED");
        checkpoint.put("resolvedAt", Instant.now().toString());
        checkpoint.put("resolvedBy", actor == null || actor.isBlank() ? "cli" : actor);
        checkpoint.put("comments", comments == null ? "" : comments);
        saveCheckpoint(checkpoint);
        appendAudit(session, checkpoint, "REJECTED", comments);

        session.put("status", "REJECTED");
        session.put("currentCheckpointId", null);
        session.put("message", comments == null || comments.isBlank() ? "Checkpoint rejected." : comments);
        session.put("updatedAt", Instant.now().toString());
        saveSession(session);
        return Map.of("checkpoint", checkpoint, "session", session);
    }

    public Map<String, Object> writeLearningResultToSession(String sessionId, String learningId, Map<String, Object> learningResult) {
        Map<String, Object> session = requireSession(sessionId);
        session.put("learningId", learningId);
        session.put("learningResult", learningResult);
        session.put("learningResultWritebackAt", Instant.now().toString());
        session.put("updatedAt", Instant.now().toString());
        saveSession(session);
        return session;
    }

    public Map<String, Object> getMemoryReinforcementStatus(String sessionId) {
        Map<String, Object> session = requireSession(sessionId);
        @SuppressWarnings("unchecked")
        Map<String, Object> reinforcement = (Map<String, Object>) session.get("memoryReinforcement");
        if (reinforcement == null) {
            return Map.of(
                    "sessionId", sessionId,
                    "status", "NOT_AVAILABLE",
                    "message", "memory reinforcement data not found"
            );
        }
        return Map.of(
                "sessionId", sessionId,
                "status", "AVAILABLE",
                "memoryReinforcement", reinforcement
        );
    }

    public Map<String, Object> getCompletionSummary(String sessionId) {
        Map<String, Object> session = requireSession(sessionId);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("sessionId", sessionId);
        summary.put("projectId", session.getOrDefault("projectId", "unknown"));
        summary.put("status", session.getOrDefault("status", "UNKNOWN"));
        summary.put("requirement", session.getOrDefault("requirement", ""));
        summary.put("message", session.getOrDefault("message", ""));
        summary.put("completionReport", session.getOrDefault("completionReport", Map.of()));
        summary.put("autoLearning", session.getOrDefault("autoLearning", Map.of()));
        summary.put("learningId", session.getOrDefault("learningId", ""));
        summary.put("learningResult", session.getOrDefault("learningResult", Map.of()));
        summary.put("memoryReinforcement", session.getOrDefault("memoryReinforcement", Map.of()));
        summary.put("updatedAt", session.getOrDefault("updatedAt", ""));
        return summary;
    }

    private Map<String, Object> advanceAfterSpecApproval(Map<String, Object> session, Map<String, Object> checkpoint) {
        String specId = String.valueOf(session.get("specId"));
        Specification spec = specEngine.approveSpec(specId, String.valueOf(checkpoint.get("resolvedBy")), String.valueOf(checkpoint.get("comments")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> knowledgeRefs = (List<Map<String, Object>>) session.getOrDefault("knowledgeRefs", List.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> memoryContext = (Map<String, Object>) session.getOrDefault("memoryContext", Map.of());
        ConfidenceScore evaluated = confidenceEngine.evaluate(spec.rawRequirement(), knowledgeRefs, memoryContext, confidenceThreshold);

        session.put("spec", spec);
        session.put("confidenceScore", evaluated.value());
        session.put("confidenceBreakdown", evaluated.dimensions());
        session.put("nmiReport", evaluated.report());
        session.put("updatedAt", Instant.now().toString());

        if (!confidenceEngine.canProceed(evaluated, confidenceThreshold)) {
            session.put("status", "NMI_WAIT");
            session.put("message", evaluated.report());
            saveSession(session);
            return Map.of("checkpoint", checkpoint, "session", session);
        }

        session.put("status", "PLANNING");
        Map<String, Object> taskPlan = planningEngine.createTaskPlan(spec, evaluated);
        session.put("taskPlan", taskPlan);
        session.put("taskPlanId", taskPlan.get("planId"));

        String taskCheckpointId = newId("cp-");
        Map<String, Object> taskCheckpoint = checkpointMap(taskCheckpointId, String.valueOf(session.get("sessionId")), "TASK_APPROVAL", "PENDING", taskPlan);
        saveCheckpoint(taskCheckpoint);

        session.put("status", "TASK_PENDING");
        session.put("currentCheckpointId", taskCheckpointId);
        session.put("message", "Task plan generated. Awaiting Task Approval.");
        saveSession(session);
        return Map.of("checkpoint", taskCheckpoint, "session", session);
    }

    private Map<String, Object> checkpointMap(String id, String sessionId, String type, String status, Map<String, Object> payload) {
        Map<String, Object> checkpoint = new LinkedHashMap<>();
        checkpoint.put("checkpointId", id);
        checkpoint.put("sessionId", sessionId);
        checkpoint.put("type", type);
        checkpoint.put("status", status);
        checkpoint.put("payload", payload);
        checkpoint.put("triggeredAt", Instant.now().toString());
        checkpoint.put("resolvedAt", null);
        checkpoint.put("resolvedBy", null);
        checkpoint.put("comments", null);
        return checkpoint;
    }

    private List<Map<String, Object>> safeKnowledgeSearch(String projectId, String requirement) {
        try {
            List<Map<String, Object>> result = knowledgeEngineClient.search(projectId, requirement, 5);
            return result == null ? List.of() : result;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Map<String, Object> safeMemoryContext(String projectId) {
        try {
            Map<String, Object> result = memoryEngineClient.getContext(projectId);
            return result == null ? Map.of() : result;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private void prepareStateDirectories() {
        try {
            Files.createDirectories(stateRoot.resolve("sessions"));
            Files.createDirectories(stateRoot.resolve("checkpoints"));
            Files.createDirectories(stateRoot.resolve("audit"));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to prepare runtime state directories", ex);
        }
    }

    private void prepareProjectWorkspace(Path projectRoot) {
        try {
            Files.createDirectories(projectRoot.resolve(".ai-project/specs"));
            Files.createDirectories(projectRoot.resolve(".ai-project/audit"));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to prepare project workspace", ex);
        }
    }

    private void saveSession(Map<String, Object> session) {
        sessions.put(String.valueOf(session.get("sessionId")), session);
        writeJson(stateRoot.resolve("sessions").resolve(session.get("sessionId") + ".json"), session);
    }

    private void saveCheckpoint(Map<String, Object> checkpoint) {
        checkpoints.put(String.valueOf(checkpoint.get("checkpointId")), checkpoint);
        writeJson(stateRoot.resolve("checkpoints").resolve(checkpoint.get("checkpointId") + ".json"), checkpoint);
    }

    private Map<String, Object> loadCheckpoint(Path path) {
        try {
            Map<String, Object> checkpoint = objectMapper.readValue(path.toFile(), new TypeReference<>() { });
            checkpoints.put(String.valueOf(checkpoint.get("checkpointId")), checkpoint);
            return checkpoint;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load checkpoint " + path, ex);
        }
    }

    private Map<String, Object> requireSession(String sessionId) {
        Map<String, Object> session = getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        return session;
    }

    private Map<String, Object> requireCheckpoint(String checkpointId) {
        Map<String, Object> checkpoint = checkpoints.get(checkpointId);
        if (checkpoint != null) {
            return checkpoint;
        }
        Path path = stateRoot.resolve("checkpoints").resolve(checkpointId + ".json");
        if (Files.exists(path)) {
            return loadCheckpoint(path);
        }
        throw new IllegalArgumentException("Checkpoint not found: " + checkpointId);
    }

    private void appendAudit(Map<String, Object> session, Map<String, Object> checkpoint, String action, String comments) {
        Path auditFile = Path.of(String.valueOf(session.get("projectRoot"))).resolve(".ai-project/audit/checkpoint-audit.jsonl");
        try {
            Files.createDirectories(auditFile.getParent());
            Map<String, Object> auditEntry = Map.of(
                    "timestamp", Instant.now().toString(),
                    "action", action,
                    "sessionId", session.get("sessionId"),
                    "checkpointId", checkpoint.get("checkpointId"),
                    "type", checkpoint.get("type"),
                    "comments", comments == null ? "" : comments
            );
            Files.writeString(auditFile, objectMapper.writeValueAsString(auditEntry) + System.lineSeparator(),
                    Files.exists(auditFile) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to append checkpoint audit", ex);
        }
    }

    private void appendAiAudit(Map<String, Object> session, Map<String, Object> execution) {
        Path auditFile = Path.of(String.valueOf(session.get("projectRoot"))).resolve(".ai-project/audit/ai-session-audit.jsonl");
        try {
            Files.createDirectories(auditFile.getParent());
            Map<String, Object> entry = Map.of(
                    "timestamp", Instant.now().toString(),
                    "sessionId", session.get("sessionId"),
                    "status", execution.get("status"),
                    "ai", execution.getOrDefault("ai", Map.of()),
                    "testing", execution.getOrDefault("testing", Map.of()),
                    "review", execution.getOrDefault("review", Map.of())
            );
            Files.writeString(auditFile, objectMapper.writeValueAsString(entry) + System.lineSeparator(),
                    Files.exists(auditFile) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to append AI session audit", ex);
        }
    }

    private void writeJson(Path path, Map<String, Object> payload) {
        try {
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), payload);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist runtime state: " + path, ex);
        }
    }

    private String newId(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private String summarize(String content) {
        String normalized = content.replace('\n', ' ').trim();
        return normalized.length() > 160 ? normalized.substring(0, 160) + "..." : normalized;
    }

    /**
     * Phase 5-2: 完成會話並觸發自動學習
     */
    private void completeSessionWithLearning(Map<String, Object> session) {
        try {
            // 生成完成報告
            SessionCompletionReport report = completionReportService.buildCompletionReport(session);
            session.put("completionReport", report.toMap());

            // 觸發自動學習
            Map<String, Object> learningResult = completionReportService.triggerAutoLearning(session, report);
            if (learningResult != null && !learningResult.containsKey("error")) {
                session.put("autoLearning", learningResult);
            }

            // Phase 5-3: 完成後自動強化關聯記憶（fail-soft）
            session.put("memoryReinforcement", reinforceMemorySignals(session));

            // 記錄完成審計
            appendCompletionAudit(session, report);
        } catch (Exception ex) {
            // 學習流程失敗不應該影響會話完成
            session.putIfAbsent("completionReport", Map.of(
                    "sessionId", String.valueOf(session.getOrDefault("sessionId", "unknown")),
                    "status", "ERROR"
            ));
            session.putIfAbsent("memoryReinforcement", Map.of(
                    "enabled", autoReinforceMemory,
                    "attempted", 0,
                    "reinforced", 0
            ));
            session.put("learningError", ex.getMessage());
        }
    }

    private void appendCompletionAudit(Map<String, Object> session, SessionCompletionReport report) {
        Path auditFile = Path.of(String.valueOf(session.get("projectRoot"))).resolve(".ai-project/audit/session-completion-audit.jsonl");
        try {
            Files.createDirectories(auditFile.getParent());
            Map<String, Object> auditEntry = Map.of(
                    "timestamp", Instant.now().toString(),
                    "sessionId", report.sessionId(),
                    "requirement", report.requirement(),
                    "specTitle", report.specTitle(),
                    "confidenceScore", report.confidenceScore(),
                    "executionStatus", report.executionStatus(),
                    "keyLearnings", report.keyLearnings(),
                    "memoryReinforcement", session.getOrDefault("memoryReinforcement", Map.of())
            );
            Files.writeString(auditFile, objectMapper.writeValueAsString(auditEntry) + System.lineSeparator(),
                    Files.exists(auditFile) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
        } catch (IOException ex) {
            // 審計失敗不應該影響會話完成
        }
    }

    private Map<String, Object> reinforceMemorySignals(Map<String, Object> session) {
        if (!autoReinforceMemory) {
            return Map.of("enabled", false, "attempted", 0, "reinforced", 0, "message", "memory auto reinforcement disabled");
        }

        List<String> memoryIds = extractMemoryIds(session);
        if (memoryIds.isEmpty()) {
            return Map.of("enabled", true, "attempted", 0, "reinforced", 0, "message", "no memory ids found in context");
        }

        List<String> reinforcedIds = new ArrayList<>();
        List<String> failedIds = new ArrayList<>();
        List<Map<String, Object>> details = new ArrayList<>();

        for (String memoryId : memoryIds.stream().limit(reinforceMaxItems).toList()) {
            try {
                Map<String, Object> reinforced = memoryEngineClient.reinforce(memoryId);
                reinforcedIds.add(memoryId);
                details.add(Map.of(
                        "memoryId", memoryId,
                        "status", "REINFORCED",
                        "result", reinforced == null ? Map.of() : reinforced
                ));
            } catch (Exception ex) {
                failedIds.add(memoryId);
                details.add(Map.of(
                        "memoryId", memoryId,
                        "status", "FAILED",
                        "error", ex.getMessage() == null ? "unknown" : ex.getMessage()
                ));
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("enabled", true);
        summary.put("attempted", details.size());
        summary.put("reinforced", reinforcedIds.size());
        summary.put("failed", failedIds.size());
        summary.put("reinforcedIds", reinforcedIds);
        summary.put("failedIds", failedIds);
        summary.put("details", details);
        return summary;
    }

    private List<String> extractMemoryIds(Map<String, Object> session) {
        @SuppressWarnings("unchecked")
        Map<String, Object> memoryContext = (Map<String, Object>) session.getOrDefault("memoryContext", Map.of());
        List<String> ids = new ArrayList<>();
        for (Object value : memoryContext.values()) {
            if (!(value instanceof List<?> list)) {
                continue;
            }
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> rawMap)) {
                    continue;
                }
                Object id = rawMap.get("id");
                if (id != null && !String.valueOf(id).isBlank()) {
                    ids.add(String.valueOf(id));
                }
            }
        }
        return ids.stream().distinct().toList();
    }
}
