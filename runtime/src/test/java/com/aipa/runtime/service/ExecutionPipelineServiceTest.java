package com.aipa.runtime.service;

import com.aipa.agent.AIAdapterRegistry;
import com.aipa.agent.adapter.ClaudeAdapter;
import com.aipa.agent.adapter.OllamaAdapter;
import com.aipa.workflow.review.ReviewEngineImpl;
import com.aipa.workflow.testing.TestingEngineImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionPipelineServiceTest {

    @Test
    void shouldProducePrReadyExecutionResult() {
        ExecutionPipelineService service = new ExecutionPipelineService(
                new AIAdapterRegistry(List.of(new OllamaAdapter(), new ClaudeAdapter())),
                new TestingEngineImpl(),
                new ReviewEngineImpl(),
                new GitService()
        );

        Map<String, Object> result = service.execute(
                "新增案件提醒功能",
                "# spec",
                List.of(Map.of("title", "Order API", "category", "API", "content", "Order endpoint")),
                Map.of("architecture", List.of(Map.of("content", "Layered"))),
                "s-test"
        );

        assertEquals("PR_READY", result.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, Object> ai = (Map<String, Object>) result.get("ai");
        assertEquals("ollama", ai.get("provider"));
        @SuppressWarnings("unchecked")
        Map<String, Object> review = (Map<String, Object>) result.get("review");
        assertEquals("PASS", review.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, Object> prPreview = (Map<String, Object>) result.get("prPreview");
        assertTrue(String.valueOf(prPreview.get("branch")).startsWith("aipa/feature/"));
        assertTrue(String.valueOf(prPreview.get("prUrl")).contains("https://example.local/aipa/pull/"));
    }
}

