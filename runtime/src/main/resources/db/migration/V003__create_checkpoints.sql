-- AIPA Studio Schema Migration V003
-- 建立 checkpoints 表

CREATE TABLE IF NOT EXISTS checkpoints (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    session_id      VARCHAR(36)  NOT NULL,
    type            VARCHAR(50)  NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    payload_json    TEXT,
    triggered_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at     TIMESTAMP,
    resolved_by     VARCHAR(255),
    comments        TEXT
);
