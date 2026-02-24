-- V9: 簡化 reminder_locks 資料表結構
--
-- 目的：移除多餘欄位，僅保留分散式鎖定的核心功能（lock_key 唯一約束）

DROP TABLE IF EXISTS reminder_locks;

CREATE TABLE reminder_locks (
    lock_key VARCHAR(100) PRIMARY KEY COMMENT '鎖定鍵（唯一，防止重複發送）',
    locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '鎖定時間'
);

CREATE INDEX idx_locks_locked_at ON reminder_locks(locked_at);
