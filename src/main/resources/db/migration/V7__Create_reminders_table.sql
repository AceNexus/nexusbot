-- V7: 建立提醒相關資料表
--
-- 目的：支援提醒設定、發送記錄與分散式鎖定功能
--
-- 包含三張表：
-- 1. reminders      — 提醒設定表
-- 2. reminder_logs  — 提醒發送記錄表
-- 3. reminder_locks — 防重複發送鎖定表

-- 1. 提醒設定表
CREATE TABLE reminders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '提醒 ID，自動遞增',
    room_id VARCHAR(100) NOT NULL COMMENT '聊天室 ID',
    content TEXT NOT NULL COMMENT '提醒內容',
    reminder_time TIMESTAMP NOT NULL COMMENT '提醒時間',
    repeat_type VARCHAR(20) DEFAULT 'ONCE' COMMENT '重複類型：ONCE, DAILY, WEEKLY',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '狀態：ACTIVE, PAUSED, COMPLETED',
    created_by VARCHAR(100) NOT NULL COMMENT '建立者（LINE userId）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間'
);

-- 2. 提醒發送記錄表
CREATE TABLE reminder_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '記錄 ID，自動遞增',
    reminder_id BIGINT NOT NULL COMMENT '提醒 ID',
    room_id VARCHAR(100) NOT NULL COMMENT '聊天室 ID',
    sent_time TIMESTAMP NOT NULL COMMENT '發送時間',
    status VARCHAR(20) NOT NULL COMMENT '發送狀態：SENT, FAILED',
    error_message TEXT COMMENT '錯誤訊息（發送失敗時記錄）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間'
);

-- 3. 防重複發送鎖定表
CREATE TABLE reminder_locks (
    reminder_id BIGINT NOT NULL COMMENT '提醒 ID',
    scheduled_time TIMESTAMP NOT NULL COMMENT '預定發送時間',
    lock_key VARCHAR(100) NOT NULL UNIQUE COMMENT '防重複鍵（唯一約束）',
    locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '鎖定時間',
    PRIMARY KEY (reminder_id, scheduled_time)
);

-- 索引
CREATE INDEX idx_reminders_time_status ON reminders(reminder_time, status);
CREATE INDEX idx_reminders_room ON reminders(room_id);
CREATE INDEX idx_logs_reminder ON reminder_logs(reminder_id);
CREATE INDEX idx_logs_room ON reminder_logs(room_id);
CREATE UNIQUE INDEX idx_locks_key ON reminder_locks(lock_key);
