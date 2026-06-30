package com.aipa.runtime.persistence;

import com.aipa.runtime.context.ProjectContextHolder;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 * ProjectSpecification — JPA Specification 基類
 *
 * 所有業務 Repository 應使用此類的子類來構建查詢，確保自動加入 project_id 過濾。
 * 這是應用層租戶隔離的核心機制。
 */
public abstract class ProjectSpecification<T> implements Specification<T> {

    protected final ProjectContextHolder contextHolder;

    public ProjectSpecification(ProjectContextHolder contextHolder) {
        this.contextHolder = contextHolder;
    }

    /**
     * 構建最終 Specification，自動加入 project_id 過濾
     */
    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        String projectId = contextHolder.getProjectIdOrNull();

        Predicate businessPredicate = buildBusinessPredicate(root, query, cb);

        if (projectId == null) {
            return businessPredicate;
        }

        // 假設所有實體都有 project_id 欄位
        Predicate projectIdPredicate = cb.equal(root.get("projectId"), projectId);

        if (businessPredicate == null) {
            return projectIdPredicate;
        }

        return cb.and(projectIdPredicate, businessPredicate);
    }

    /**
     * 由子類實現，返回業務邏輯的 Predicate（不包括 project_id 過濾）
     */
    protected abstract Predicate buildBusinessPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
}

