package com.aipa.agent;

/**
 * AIAdapter — AI 供應商介面（Adapter Pattern）
 * 所有 AI 供應商必須實作此介面
 */
public interface AIAdapter {
    String name();
    AdapterType type();
    boolean isAvailable();
    AIResponse generate(AIRequest request);
    int estimateTokens(String text);
}
