package com.aipa.agent;

import java.util.List;

/**
 * AIExecutionResult — AI Adapter Registry 的執行結果，包含 fallback 軌跡
 */
public record AIExecutionResult(
        AIResponse response,
        List<String> attemptedProviders,
        boolean fallbackUsed
) {
}

