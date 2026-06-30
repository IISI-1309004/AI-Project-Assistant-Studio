-- AIPA Studio Schema Migration V010
-- 強化多租戶隔離：為所有實體表添加 project_id 外鍵約束

-- 為 sessions 表添加外鍵約束（如果還沒有的話）
-- 注意：SQLite 不支持 ALTER TABLE 直接添加外鍵，暫時只記錄關係

-- 為 checkpoints 表添加外鍵約束（如果有的話）
-- CREATE INDEX IF NOT EXISTS idx_checkpoint_project ON checkpoints(project_id);

-- 為知識庫添加專案隔離索引（已存在於 V004）

-- 為記憶庫添加專案隔離索引（已存在於 V005）

-- 創建專案統計視圖
CREATE VIEW IF NOT EXISTS v_project_stats AS
SELECT
    p.id,
    p.name,
    p.status,
    COUNT(DISTINCT s.id) as session_count,
    COUNT(DISTINCT k.id) as knowledge_count,
    COUNT(DISTINCT m.id) as memory_count
FROM projects p
LEFT JOIN sessions s ON p.id = s.project_id
LEFT JOIN knowledge_items k ON p.id = k.project_id
LEFT JOIN memory_entries m ON p.id = m.project_id
GROUP BY p.id, p.name, p.status;

-- 為會話表添加索引（提高查詢性能）
CREATE INDEX IF NOT EXISTS idx_session_project_status ON sessions(project_id, status);
CREATE INDEX IF NOT EXISTS idx_session_created_at ON sessions(created_at DESC);

