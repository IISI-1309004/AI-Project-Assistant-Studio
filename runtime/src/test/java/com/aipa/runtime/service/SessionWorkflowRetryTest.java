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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SessionWorkflowRetryTest {

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

        ExecutionPipelineService flakyExecution = new FlakyExecutionPipelineService();
        service = new SessionWorkflowService(
                new KnowledgeEngineClient(normalizedBaseUrl),
                new MemoryEngineClient(normalizedBaseUrl),
                new SpecEngineImpl(new ObjectMapper()),
                new ConfidenceEngineImpl(),
                new PlanningEngineImpl(),
                flakyExecution,
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
    void shouldRetryExecutionUntilPrReady() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("[{\"id\":\"k1\",\"title\":\"Order API\",\"category\":\"API\",\"content\":\"Order endpoint\"},"
                        + "{\"id\":\"k2\",\"title\":\"Service Layer\",\"category\":\"ARCHITECTURE\",\"content\":\"Layered design\"}]")
        );
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"coding_style\":[],\"architecture\":[{\"id\":\"m1\",\"content\":\"Layered\"}],\"business_rules\":[],\"decisions\":[],\"patterns\":[],\"total\":1}")
        );

        Path projectRoot = tempDir.resolve("retry-project");
        Files.createDirectories(projectRoot);

        Map<String, Object> session = service.createSession("demo", projectRoot.toString(), "新增案件提醒功能");
        List<Map<String, Object>> checkpoints = service.listPendingCheckpoints(String.valueOf(session.get("sessionId")));
        service.approveCheckpoint(String.valueOf(checkpoints.get(0).get("checkpointId")), "tester", "spec ok");

        List<Map<String, Object>> taskCheckpoints = service.listPendingCheckpoints(String.valueOf(session.get("sessionId")));
        @SuppressWarnings("unchecked")
        Map<String, Object> taskApproval = (Map<String, Object>) service.approveCheckpoint(String.valueOf(taskCheckpoints.get(0).get("checkpointId")), "tester", "task ok");
        @SuppressWarnings("unchecked")
        Map<String, Object> updatedSession = (Map<String, Object>) taskApproval.get("session");

        assertEquals("PR_PENDING", updatedSession.get("status"));
        assertEquals(3, updatedSession.get("executionAttempts"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) updatedSession.get("executionHistory");
        assertNotNull(history);
        assertEquals(3, history.size());
        assertEquals("TEST_FAILED", history.get(0).get("status"));
        assertEquals("REVIEW_FAILED", history.get(1).get("status"));
        assertEquals("PR_READY", history.get(2).get("status"));
    }

    private static class FlakyExecutionPipelineService extends ExecutionPipelineService {
        private final AtomicInteger attempt = new AtomicInteger(0);

        FlakyExecutionPipelineService() {
            super(
                    new AIAdapterRegistry(List.of(new OllamaAdapter(), new ClaudeAdapter())),
                    new TestingEngineImpl(),
                    new ReviewEngineImpl(),
                    new GitService()
            );
        }

        @Override
        public Map<String, Object> execute(String requirement, String specContent,
                                           List<Map<String, Object>> knowledgeRefs,
                                           Map<String, Object> memoryContext,
                                           String sessionId) {
            int current = attempt.incrementAndGet();
            if (current == 1) {
                return Map.of(
                        "status", "TEST_FAILED",
                        "ai", Map.of("provider", "ollama"),
                        "testing", Map.of("status", "FAIL"),
                        "review", Map.of("status", "PASS")
                );
            }
            if (current == 2) {
                return Map.of(
                        "status", "REVIEW_FAILED",
                        "ai", Map.of("provider", "claude"),
                        "testing", Map.of("status", "PASS"),
                        "review", Map.of("status", "FAIL")
                );
            }
            return Map.of(
                    "status", "PR_READY",
                    "ai", Map.of("provider", "ollama"),
                    "testing", Map.of("status", "PASS"),
                    "review", Map.of("status", "PASS"),
                    "prPreview", Map.of("title", "feat: " + requirement, "branch", "aipa/feature/retry", "prUrl", "https://example.local/aipa/pull/retry")
            );
        }
    }
}

