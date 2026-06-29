-- AIPA Studio Schema Migration V004
-- 建立 knowledge_items 表

CREATE TABLE IF NOT EXISTS knowledge_items (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    project_id  VARCHAR(36)  NOT NULL,
    category    VARCHAR(50)  NOT NULL,
    title       VARCHAR(500) NOT NULL,
    content     TEXT         NOT NULL,
    source_type VARCHAR(50)  NOT NULL DEFAULT 'MANUAL',
    source_ref  TEXT,
    tags        TEXT,
    confidence  INTEGER      NOT NULL DEFAULT 80,
    vector_id   VARCHAR(255),
    version     INTEGER      NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_project ON knowledge_items(project_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_category ON knowledge_items(project_id, category);
