package com.aipa.runtime.persistence;

import com.aipa.runtime.context.ProjectContextHolder;
import com.aipa.runtime.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SessionRepository — 會話數據訪問層（多租戶隔離示例）
 *
 * 繼承 JpaSpecificationExecutor，使用 ProjectSpecification 自動過濾 project_id。
 *
 * 使用示例：
 *
 *   // 查詢當前項目的所有會話
 *   Specification<Session> spec = new SessionsByStatusSpec(contextHolder, "COMPLETED");
 *   List<Session> sessions = repository.findAll(spec);
 *
 *   // 自動會附加: WHERE project_id = ? AND status = ?
 */
@Repository
public interface SessionRepository extends
    JpaRepository<Session, String>,
    JpaSpecificationExecutor<Session> {

    /**
     * 根據會話 ID 查詢（不需要 project_id 過濾，ID 全局唯一）
     */
    Optional<Session> findById(String id);

    /**
     * 查詢特定項目的所有會話
     *
     * 使用方式：
     *   List<Session> sessions = repository.findByProjectId(contextHolder.getProjectId());
     */
    List<Session> findByProjectId(String projectId);

    /**
     * 計算特定項目的會話數
     */
    long countByProjectId(String projectId);
}

