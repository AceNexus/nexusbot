-- 簡化 reminder_locks 表結構
-- 移除多餘欄位，只保留基本的分散式鎖功能

-- 先刪除舊表
DROP TABLE IF EXISTS reminder_locks;

-- 重新建立簡化的鎖定表
CREATE TABLE reminder_locks (
    lock_key VARCHAR(100) PRIMARY KEY,           -- 鎖定鍵（唯一）
    locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 鎖定時間
);

-- 建立過期時間索引，方便清理
CREATE INDEX idx_locks_locked_at ON reminder_locks(locked_at);