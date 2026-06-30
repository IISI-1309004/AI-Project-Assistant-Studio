package com.aipa.runtime.service;

import com.aipa.agent.AIAdapterRegistry;
import com.aipa.agent.AIExecutionResult;
import com.aipa.agent.AIRequest;
import com.aipa.workflow.review.ReviewEngineImpl;
import com.aipa.workflow.testing.TestingEngineImpl;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExecutionPipelineService {

    private final AIAdapterRegistry adapterRegistry;
    private final TestingEngineImpl testingEngine;
    private final ReviewEngineImpl reviewEngine;
    private final GitService gitService;

    public ExecutionPipelineService(
            AIAdapterRegistry adapterRegistry,
            TestingEngineImpl testingEngine,
            ReviewEngineImpl reviewEngine,
            GitService gitService
    ) {
        this.adapterRegistry = adapterRegistry;
        this.testingEngine = testingEngine;
        this.reviewEngine = reviewEngine;
        this.gitService = gitService;
    }

    public Map<String, Object> execute(String requirement, String specContent,
                                       List<Map<String, Object>> knowledgeRefs,
                                       Map<String, Object> memoryContext,
                                       String sessionId) {
        String taskSpec = (requirement == null ? "" : requirement)
                + "\n\nSpec Context:\n"
                + (specContent == null ? "" : specContent);
        AIRequest request = new AIRequest(
                taskSpec,
                summarizeKnowledge(knowledgeRefs),
                summarizeMemory(memoryContext),
                "",
                List.of("Follow layered architecture", "Prefer minimal safe changes", "Include testing notes"),
                "code+notes",
                4096
        );

        AIExecutionResult aiExecution = adapterRegistry.execute(request);
        Map<String, Object> testing = testingEngine.runPhase4Checks(requirement, aiExecution.response().content());
        Map<String, Object> review = reviewEngine.review(aiExecution.response().content(), List.of("generated/GeneratedChangePlan.java"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("executedAt", Instant.now().toString());
        result.put("ai", Map.of(
                "provider", aiExecution.response().provider(),
                "model", aiExecution.response().model(),
                "success", aiExecution.response().success(),
                "attemptedProviders", aiExecution.attemptedProviders(),
                "fallbackUsed", aiExecution.fallbackUsed(),
                "content", aiExecution.response().content(),
                "errorMessage", aiExecution.response().errorMessage()
        ));
        result.put("testing", testing);
        result.put("review", review);
        result.put("status", deriveStatus(aiExecution, testing, review));
        result.put("prPreview", gitService.createPrPreview(requirement, aiExecution.response().provider(), sessionId));
        return result;
    }

    private String summarizeKnowledge(List<Map<String, Object>> knowledgeRefs) {
        return knowledgeRefs.stream()
                .limit(5)
                .map(item -> String.valueOf(item.getOrDefault("title", "untitled")))
                .reduce((left, right) -> left + "; " + right)
                .orElse("no knowledge");
    }

    private String summarizeMemory(Map<String, Object> memoryContext) {
        return memoryContext.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof List<?> list && !list.isEmpty())
                .map(Map.Entry::getKey)
                .reduce((left, right) -> left + ", " + right)
                .orElse("no memory");
    }

    private String deriveStatus(AIExecutionResult aiExecution, Map<String, Object> testing, Map<String, Object> review) {
        if (!aiExecution.response().success()) {
            return "AI_FAILED";
        }
        if (!"PASS".equals(String.valueOf(testing.get("status")))) {
            return "TEST_FAILED";
        }
        if (!"PASS".equals(String.valueOf(review.get("status")))) {
            return "REVIEW_FAILED";
        }
        return "PR_READY";
    }

}

