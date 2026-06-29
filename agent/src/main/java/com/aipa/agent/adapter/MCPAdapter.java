package com.aipa.agent.adapter;

import com.aipa.agent.*;
import org.springframework.stereotype.Component;

/** Phase 1 骨架 — MCP Adapter（Phase 4 實作） */
@Component
public class MCPAdapter implements AIAdapter {
    @Override public String name() { return "MCP"; }
    @Override public AdapterType type() { return AdapterType.MCP; }
    @Override public boolean isAvailable() { return false; }
    @Override public AIResponse generate(AIRequest request) { return AIResponse.notImplemented(); }
    @Override public int estimateTokens(String text) { return text.length() / 4; }
}
