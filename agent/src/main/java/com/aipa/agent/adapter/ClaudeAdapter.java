package com.aipa.agent.adapter;

import com.aipa.agent.*;
import org.springframework.stereotype.Component;

/** Phase 1 骨架 — Claude Adapter（Phase 4 實作） */
@Component
public class ClaudeAdapter implements AIAdapter {
    @Override public String name() { return "Claude"; }
    @Override public AdapterType type() { return AdapterType.CLAUDE; }
    @Override public boolean isAvailable() { return false; } // Phase 4 啟用
    @Override public AIResponse generate(AIRequest request) { return AIResponse.notImplemented(); }
    @Override public int estimateTokens(String text) { return text.length() / 4; }
}
