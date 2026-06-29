package com.aipa.scanner;

import java.util.List;

/**
 * ScanConfig — 掃描設定
 */
public record ScanConfig(
        List<String> excludePatterns,
        boolean includeTestSources,
        int maxFileSizeKb
) {
    public static ScanConfig defaults() {
        return new ScanConfig(
                List.of("**/target/**", "**/build/**", "**/.git/**", "**/node_modules/**"),
                false,
                512
        );
    }
}
