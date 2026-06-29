package com.aipa.agent.adapter;

import com.aipa.agent.*;
import org.springframework.stereotype.Component;

/** Phase 1 骨架 — Gemini Adapter（Phase 4 實作） */
@Component
public class GeminiAdapter implements AIAdapter {
    @Override public String name() { return "Gemini"; }
    @Override public AdapterType type() { return AdapterType.GEMINI; }
    @Override public boolean isAvailable() { return false; }
    @Override public AIResponse generate(AIRequest request) { return AIResponse.notImplemented(); }
    @Override public int estimateTokens(String text) { return text.length() / 4; }
}
