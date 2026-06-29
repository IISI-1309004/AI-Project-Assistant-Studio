package com.aipa.agent;

import java.util.List;

/**
 * AIRequest — 傳送給 AI 供應商的請求（標準格式）
 */
public record AIRequest(
        String taskSpec,
        String contextKnowledge,
        String contextMemory,
        String codeContext,
        List<String> constraints,
        String outputFormat,
        int maxTokens
) {
    public static AIRequest of(String taskSpec) {
        return new AIRequest(taskSpec, "", "", "", List.of(), "code", 4096);
    }
}
