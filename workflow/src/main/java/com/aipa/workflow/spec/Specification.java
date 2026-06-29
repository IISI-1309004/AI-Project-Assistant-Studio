package com.aipa.workflow.spec;

import java.time.Instant;

/**
 * Specification — 規格文件實體（Phase 1 骨架）
 */
public record Specification(
        String id,
        String projectId,
        String sessionId,
        SpecType type,
        SpecStatus status,
        String title,
        String rawRequirement,
        String content,   // Markdown 格式的完整規格
        int confidenceScore,
        Instant createdAt
) {
    public static Specification notImplemented(String requirement) {
        return new Specification(
                java.util.UUID.randomUUID().toString(),
                "unknown",
                "unknown",
                SpecType.FEATURE,
                SpecStatus.DRAFT,
                "Phase 1 Skeleton",
                requirement,
                "# TODO Phase 3: Specification Engine not yet implemented",
                0,
                Instant.now()
        );
    }
}
