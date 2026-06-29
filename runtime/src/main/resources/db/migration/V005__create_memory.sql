-- AIPA Studio Schema Migration V005
-- 建立 memory_entries 表

CREATE TABLE IF NOT EXISTS memory_entries (
    id                  VARCHAR(36)  NOT NULL PRIMARY KEY,
    project_id          VARCHAR(36)  NOT NULL,
    type                VARCHAR(50)  NOT NULL,
    key                 VARCHAR(500) NOT NULL,
    content             TEXT         NOT NULL,
    strength            INTEGER      NOT NULL DEFAULT 5,
    reinforced_count    INTEGER      NOT NULL DEFAULT 0,
    source_session_id   VARCHAR(36),
    source_pr_id        VARCHAR(255),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_reinforced_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, type, key)
);

CREATE INDEX IF NOT EXISTS idx_memory_project ON memory_entries(project_id);
CREATE INDEX IF NOT EXISTS idx_memory_type ON memory_entries(project_id, type);
