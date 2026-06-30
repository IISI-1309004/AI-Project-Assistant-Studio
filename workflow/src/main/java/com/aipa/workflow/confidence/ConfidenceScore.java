package com.aipa.workflow.confidence;

import java.util.List;
import java.util.Map;

/**
 * ConfidenceScore — 信心分數值物件（Phase 3）
 */
public record ConfidenceScore(
        int value,
        Map<String, Integer> dimensions,
        List<String> missingItems,
        String report
) {
    public static final int DEFAULT_THRESHOLD = 70;

    public ConfidenceScore {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("ConfidenceScore must be between 0 and 100, got: " + value);
        }
        dimensions = Map.copyOf(dimensions);
        missingItems = List.copyOf(missingItems);
        report = report == null ? "" : report;
    }

    public boolean isAboveThreshold(int threshold) {
        return value >= threshold;
    }

    public static ConfidenceScore notImplemented() {
        return new ConfidenceScore(0, Map.of(), List.of("Confidence evaluation not implemented"), "Confidence evaluation not implemented");
    }
}
