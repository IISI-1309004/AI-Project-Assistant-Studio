package com.aipa.workflow.spec;

import org.springframework.stereotype.Service;

/**
 * SpecEngineImpl — Phase 1 骨架（Phase 3 實作業務邏輯）
 */
@Service
public class SpecEngineImpl implements SpecEngine {

    @Override
    public Specification generateSpec(String rawRequirement, SpecType type, String sessionId) {
        // TODO Phase 3：呼叫 Knowledge Engine、Memory Engine 建立上下文，生成完整規格
        return Specification.notImplemented(rawRequirement);
    }

    @Override
    public void approveSpec(String specId, String approvedBy, String comments) {
        // TODO Phase 3：更新規格狀態為 APPROVED
    }

    @Override
    public void rejectSpec(String specId, String rejectedBy, String reason) {
        // TODO Phase 3：更新規格狀態為 REJECTED
    }

    @Override
    public Specification getSpec(String specId) {
        // TODO Phase 3：從 Repository 查詢規格
        return null;
    }
}
