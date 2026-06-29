package com.aipa.agent;

/**
 * AIResponse — AI 供應商的回應（標準格式）
 */
public record AIResponse(
        String content,
        String provider,
        String model,
        int inputTokens,
        int outputTokens,
        long latencyMs,
        boolean success,
        String errorMessage
) {
    public static AIResponse notImplemented() {
        return new AIResponse(
                "// TODO Phase 4: AI Adapter not yet implemented",
                "none",
                "none",
                0, 0, 0,
                false,
                "Phase 1 skeleton — AI Adapter not yet implemented"
        );
    }

    public static AIResponse error(String message) {
        return new AIResponse("", "none", "none", 0, 0, 0, false, message);
    }
}
