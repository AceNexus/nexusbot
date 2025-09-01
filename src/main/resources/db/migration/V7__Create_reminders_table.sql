-- 建立提醒功能相關表格
-- 包含：提醒設定表、發送記錄表、防重複鎖定表

-- 1. 提醒設定表
CREATE TABLE reminders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,                    -- 提醒ID
    room_id VARCHAR(100) NOT NULL,                           -- 聊天室ID
    content TEXT NOT NULL,                                   -- 提醒內容
    reminder_time TIMESTAMP NOT NULL,                        -- 提醒時間
    repeat_type VARCHAR(20) DEFAULT 'ONCE',                  -- 重複類型：ONCE, DAILY, WEEKLY
    status VARCHAR(20) DEFAULT 'ACTIVE',                     -- 狀態：ACTIVE, PAUSED, COMPLETED
    created_by VARCHAR(100) NOT NULL,                        -- 建立者
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 發送記錄表
CREATE TABLE reminder_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reminder_id BIGINT NOT NULL,                             -- 提醒ID
    room_id VARCHAR(100) NOT NULL,                           -- 聊天室ID
    sent_time TIMESTAMP NOT NULL,                            -- 發送時間
    status VARCHAR(20) NOT NULL,                             -- SENT, FAILED
    error_message TEXT,                                      -- 錯誤訊息
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. 防重複鎖定表
CREATE TABLE reminder_locks (
    reminder_id BIGINT NOT NULL,
    scheduled_time TIMESTAMP NOT NULL,
    lock_key VARCHAR(100) NOT NULL UNIQUE,                   -- 防重複鍵
    locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (reminder_id, scheduled_time)
);

-- 索引
CREATE INDEX idx_reminders_time_status ON reminders(reminder_time, status);
CREATE INDEX idx_reminders_room ON reminders(room_id);
CREATE INDEX idx_logs_reminder ON reminder_logs(reminder_id);
CREATE INDEX idx_logs_room ON reminder_logs(room_id);
CREATE UNIQUE INDEX idx_locks_key ON reminder_locks(lock_key);