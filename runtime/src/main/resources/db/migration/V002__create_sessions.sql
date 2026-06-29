-- AIPA Studio Schema Migration V002
-- 建立 sessions 表

CREATE TABLE IF NOT EXISTS sessions (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    project_id      VARCHAR(36)  NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'CREATED',
    requirement     TEXT         NOT NULL,
    spec_id         VARCHAR(36),
    task_plan_id    VARCHAR(36),
    pr_url          TEXT,
    learning_json   TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMP
);
