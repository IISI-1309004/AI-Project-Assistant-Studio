package com.aipa.runtime.persistence;

import com.aipa.runtime.context.ProjectContextHolder;
import com.aipa.runtime.domain.Session;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * SessionsByStatusSpecification — 查詢特定狀態的會話
 *
 * 示例：展示如何使用 ProjectSpecification 自動隔離不同項目的數據。
 *
 * 使用方式：
 *   List<Session> sessions = repository.findAll(
 *     new SessionsByStatusSpecification(contextHolder, "COMPLETED")
 *   );
 *
 * 生成的 SQL：
 *   SELECT * FROM sessions
 *   WHERE project_id = 'customer-service' AND status = 'COMPLETED'
 */
public class SessionsByStatusSpecification extends ProjectSpecification<Session> {

    private final String status;

    public SessionsByStatusSpecification(ProjectContextHolder contextHolder, String status) {
        super(contextHolder);
        this.status = status;
    }

    @Override
    protected Predicate buildBusinessPredicate(Root<Session> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return cb.equal(root.get("status"), status);
    }
}

