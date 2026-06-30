package com.aipa.workflow.spec;

/**
 * SpecEngine — 規格引擎介面（Phase 1 骨架）
 */
public interface SpecEngine {
    Specification generateSpec(SpecRequest request);
    Specification approveSpec(String specId, String approvedBy, String comments);
    Specification rejectSpec(String specId, String rejectedBy, String reason);
    Specification getSpec(String specId);
}
