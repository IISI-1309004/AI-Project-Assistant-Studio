package com.aipa.scanner;

import java.util.List;

/**
 * TechStack — 偵測到的技術棧資訊（Phase 1 骨架）
 */
public record TechStack(
        String javaVersion,
        String springBootVersion,
        String buildTool,
        List<String> frameworks,
        List<String> databases
) {
    public static TechStack unknown() {
        return new TechStack("unknown", "unknown", "unknown", List.of(), List.of());
    }
}
