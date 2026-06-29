package com.aipa.workflow.spec;

/**
 * SpecEngine — 規格引擎介面（Phase 1 骨架）
 */
public interface SpecEngine {
    Specification generateSpec(String rawRequirement, SpecType type, String sessionId);
    void approveSpec(String specId, String approvedBy, String comments);
    void rejectSpec(String specId, String rejectedBy, String reason);
    Specification getSpec(String specId);
}
