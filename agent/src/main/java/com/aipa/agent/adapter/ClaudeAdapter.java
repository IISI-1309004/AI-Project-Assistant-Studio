package com.aipa.agent.adapter;

import com.aipa.agent.AIAdapter;
import com.aipa.agent.AIRequest;
import com.aipa.agent.AIResponse;
import com.aipa.agent.AdapterType;
import org.springframework.stereotype.Component;

/** Phase 4 最小實作 — Claude Adapter（fallback provider） */
@Component
public class ClaudeAdapter implements AIAdapter {
    @Override public String name() { return "Claude"; }
    @Override public AdapterType type() { return AdapterType.CLAUDE; }
    @Override public boolean isAvailable() { return true; }

    @Override
    public AIResponse generate(AIRequest request) {
        String content = "# Claude fallback plan\n"
                + "Requirement: " + request.taskSpec() + "\n"
                + "Action: update service, controller, tests, and rollout notes.\n";
        return new AIResponse(content, "claude", "simulated-fallback", estimateTokens(request.taskSpec()), estimateTokens(content), 55, true, "");
    }

    @Override public int estimateTokens(String text) { return Math.max(1, text.length() / 4); }
}
