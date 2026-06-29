package com.aipa.agent.adapter;

import com.aipa.agent.*;
import org.springframework.stereotype.Component;

/** Phase 1 骨架 — OpenAI Adapter（Phase 4 實作） */
@Component
public class OpenAIAdapter implements AIAdapter {
    @Override public String name() { return "OpenAI"; }
    @Override public AdapterType type() { return AdapterType.OPENAI; }
    @Override public boolean isAvailable() { return false; }
    @Override public AIResponse generate(AIRequest request) { return AIResponse.notImplemented(); }
    @Override public int estimateTokens(String text) { return text.length() / 4; }
}
