-- AIPA Studio Schema Migration V001
-- 建立 projects 表

CREATE TABLE IF NOT EXISTS projects (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    root_path   TEXT         NOT NULL UNIQUE,
    status      VARCHAR(50)  NOT NULL DEFAULT 'INITIALIZING',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_scan_at TIMESTAMP,
    config_json TEXT,
    dna_json    TEXT
);
