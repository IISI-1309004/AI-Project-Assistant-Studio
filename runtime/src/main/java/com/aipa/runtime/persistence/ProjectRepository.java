package com.aipa.runtime.persistence;

import com.aipa.runtime.domain.Project;
import com.aipa.runtime.domain.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ProjectRepository — 項目數據訪問層
 *
 * 注意：Project 實體是特殊的，它不被 ProjectContextHolder 過濾，
 * 因為項目本身定義了上下文。訪問單個項目時不需要上下文。
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    /**
     * 根據根目錄路徑查詢項目
     */
    Optional<Project> findByRootPath(String rootPath);

    /**
     * 根據項目 ID 查詢
     */
    Optional<Project> findById(String id);

    /**
     * 查詢所有活躍項目
     */
    List<Project> findByStatus(ProjectStatus status);

    /**
     * 查詢所有項目
     */
    List<Project> findAll();

    /**
     * 根據所有者查詢項目
     */
    @Query("SELECT p FROM Project p WHERE p.ownerId = :ownerId ORDER BY p.createdAt DESC")
    List<Project> findByOwnerId(@Param("ownerId") String ownerId);

    /**
     * 根據名稱模糊查詢
     */
    @Query("SELECT p FROM Project p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :namePart, '%')) ORDER BY p.name")
    List<Project> findByNameContaining(@Param("namePart") String namePart);

    /**
     * 檢查項目 ID 是否存在
     */
    boolean existsById(String id);

    /**
     * 檢查根目錄是否已被使用
     */
    boolean existsByRootPath(String rootPath);
}

