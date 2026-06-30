package com.aipa.agent;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class AIAdapterRegistry {

    private final List<AIAdapter> adapters;

    public AIAdapterRegistry(List<AIAdapter> adapters) {
        this.adapters = List.copyOf(adapters);
    }

    public AIExecutionResult execute(AIRequest request) {
        List<AIAdapter> ordered = adapters.stream()
                .sorted(Comparator.comparingInt(this::priority))
                .toList();
        List<String> attempts = new ArrayList<>();

        for (AIAdapter adapter : ordered) {
            attempts.add(adapter.name());
            if (!adapter.isAvailable()) {
                continue;
            }
            AIResponse response = adapter.generate(request);
            if (response != null && response.success()) {
                return new AIExecutionResult(response, attempts, attempts.size() > 1);
            }
        }

        return new AIExecutionResult(
                AIResponse.error("No available AI adapter could complete the request"),
                attempts,
                attempts.size() > 1
        );
    }

    private int priority(AIAdapter adapter) {
        return switch (adapter.type()) {
            case OLLAMA -> 0;
            case CLAUDE -> 1;
            case OPENAI -> 2;
            case GEMINI -> 3;
            case COPILOT -> 4;
            case MCP -> 5;
        };
    }
}

