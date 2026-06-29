package com.aipa.agent.adapter;

import com.aipa.agent.*;
import org.springframework.stereotype.Component;

/** Phase 1 骨架 — Copilot Adapter（Phase 4 實作） */
@Component
public class CopilotAdapter implements AIAdapter {
    @Override public String name() { return "Copilot"; }
    @Override public AdapterType type() { return AdapterType.COPILOT; }
    @Override public boolean isAvailable() { return false; }
    @Override public AIResponse generate(AIRequest request) { return AIResponse.notImplemented(); }
    @Override public int estimateTokens(String text) { return text.length() / 4; }
}
