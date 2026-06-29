package com.aipa.runtime.service;

import com.aipa.agent.AIAdapterRegistry;
import com.aipa.agent.adapter.ClaudeAdapter;
import com.aipa.agent.adapter.OllamaAdapter;
import com.aipa.workflow.confidence.ConfidenceEngineImpl;
import com.aipa.workflow.planning.PlanningEngineImpl;
import com.aipa.workflow.review.ReviewEngineImpl;
import com.aipa.workflow.spec.SpecEngineImpl;
import com.aipa.workflow.testing.TestingEngineImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SessionWorkflowServiceTest {

    @TempDir
    Path tempDir;

    private MockWebServer mockWebServer;
    private SessionWorkflowService service;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        String normalizedBaseUrl = baseUrl.substring(0, baseUrl.length() - 1);

        service = new SessionWorkflowService(
                new KnowledgeEngineClient(normalizedBaseUrl),
                new MemoryEngineClient(normalizedBaseUrl),
                new SpecEngineImpl(new ObjectMapper()),
                new ConfidenceEngineImpl(),
                new PlanningEngineImpl(),
                new ExecutionPipelineService(
                        new AIAdapterRegistry(List.of(new OllamaAdapter(), new ClaudeAdapter())),
                        new TestingEngineImpl(),
                        new ReviewEngineImpl(),
                        new GitService()
                ),
                new SessionCompletionReportService(
                        new LearningEngineClient(normalizedBaseUrl),
                        new ObjectMapper(),
                        true,
                        true
                ),
                new ObjectMapper(),
                70,
                3,
                true,
                3,
                tempDir.resolve("runtime-state").toString()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldRunSpecToTaskApprovalWorkflow() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("[{\"id\":\"k1\",\"title\":\"Order API\",\"category\":\"API\",\"content\":\"Order endpoint\"},"
                        + "{\"id\":\"k2\",\"title\":\"Service Layer\",\"category\":\"ARCHITECTURE\",\"content\":\"Layered design\"}]")
        );
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"coding_style\":[],\"architecture\":[{\"id\":\"m1\",\"content\":\"Layered\"}],\"business_rules\":[],\"decisions\":[],\"patterns\":[],\"total\":1}")
        );

        Path projectRoot = tempDir.resolve("demo-project");
        Files.createDirectories(projectRoot);

        Map<String, Object> session = service.createSession("demo", projectRoot.toString(), "新增案件提醒功能");
        assertEquals("SPEC_PENDING", session.get("status"));
        assertNotNull(session.get("specId"));
        assertNotNull(session.get("currentCheckpointId"));
        assertTrue(Files.exists(projectRoot.resolve(".ai-project/specs")));

        List<Map<String, Object>> checkpoints = service.listPendingCheckpoints(String.valueOf(session.get("sessionId")));
        assertEquals(1, checkpoints.size());
        assertEquals("SPEC_APPROVAL", checkpoints.get(0).get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> approval = (Map<String, Object>) service.approveCheckpoint(String.valueOf(checkpoints.get(0).get("checkpointId")), "tester", "spec ok");
        @SuppressWarnings("unchecked")
        Map<String, Object> plannedSession = (Map<String, Object>) approval.get("session");
        assertEquals("TASK_PENDING", plannedSession.get("status"));
        assertNotNull(plannedSession.get("taskPlan"));

        List<Map<String, Object>> taskCheckpoints = service.listPendingCheckpoints(String.valueOf(session.get("sessionId")));
        assertEquals(1, taskCheckpoints.size());
        assertEquals("TASK_APPROVAL", taskCheckpoints.get(0).get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> finalApproval = (Map<String, Object>) service.approveCheckpoint(String.valueOf(taskCheckpoints.get(0).get("checkpointId")), "tester", "task ok");
        @SuppressWarnings("unchecked")
        Map<String, Object> finalSession = (Map<String, Object>) finalApproval.get("session");
        assertEquals("PR_PENDING", finalSession.get("status"));
        assertTrue(String.valueOf(finalSession.get("phase4Message")).contains("AI pipeline executed"));
        @SuppressWarnings("unchecked")
        Map<String, Object> execution = (Map<String, Object>) finalSession.get("execution");
        assertNotNull(execution);
        assertEquals("PR_READY", execution.get("status"));
        assertEquals(1, finalSession.get("executionAttempts"));

        List<Map<String, Object>> prCheckpoints = service.listPendingCheckpoints(String.valueOf(session.get("sessionId")));
        assertEquals(1, prCheckpoints.size());
        assertEquals("PR_APPROVAL", prCheckpoints.get(0).get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> prApproval = (Map<String, Object>) service.approveCheckpoint(String.valueOf(prCheckpoints.get(0).get("checkpointId")), "tester", "pr ok");
        @SuppressWarnings("unchecked")
        Map<String, Object> completedSession = (Map<String, Object>) prApproval.get("session");
        assertEquals("COMPLETED", completedSession.get("status"));
        assertNotNull(completedSession.get("completionReport"));
        assertNotNull(completedSession.get("memoryReinforcement"));

        @SuppressWarnings("unchecked")
        Map<String, Object> reinforcement = (Map<String, Object>) completedSession.get("memoryReinforcement");
        assertEquals(true, reinforcement.get("enabled"));

        Path auditFile = projectRoot.resolve(".ai-project/audit/checkpoint-audit.jsonl");
        assertTrue(Files.exists(auditFile));
        String auditContent = Files.readString(auditFile);
        assertTrue(auditContent.contains("APPROVED"));

        Path aiAuditFile = projectRoot.resolve(".ai-project/audit/ai-session-audit.jsonl");
        assertTrue(Files.exists(aiAuditFile));
    }
}

