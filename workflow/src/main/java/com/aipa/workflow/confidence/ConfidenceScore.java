package com.aipa.workflow.confidence;

/**
 * ConfidenceScore — 信心分數值物件（Phase 1 骨架）
 */
public record ConfidenceScore(int value) {
    public static final int DEFAULT_THRESHOLD = 70;

    public ConfidenceScore {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("ConfidenceScore must be between 0 and 100, got: " + value);
        }
    }

    public boolean isAboveThreshold(int threshold) {
        return value >= threshold;
    }

    public static ConfidenceScore notImplemented() {
        return new ConfidenceScore(0);
    }
}
