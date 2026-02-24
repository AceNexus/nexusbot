-- V8: 建立 reminder_states 資料表
--
-- 目的：支援多實例伺服器環境下的提醒建立流程狀態管理
--
-- 設計考量：
-- 1. 原本使用 ConcurrentHashMap 在單一 JVM 記憶體中，多實例環境會導致狀態不同步
-- 2. 改用資料庫存儲，確保所有伺服器實例都能存取相同的狀態資料
-- 3. 設有過期時間（30 分鐘），避免殘留資料累積

CREATE TABLE reminder_states (
    room_id VARCHAR(100) PRIMARY KEY COMMENT '聊天室 ID',
    step VARCHAR(50) NOT NULL COMMENT '當前步驟：WAITING_FOR_REPEAT_TYPE, WAITING_FOR_TIME, WAITING_FOR_CONTENT',
    repeat_type VARCHAR(20) COMMENT '重複類型：ONCE, DAILY, WEEKLY',
    reminder_time TIMESTAMP COMMENT '暫存的提醒時間',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    expires_at TIMESTAMP NOT NULL COMMENT '狀態過期時間（30 分鐘後自動清除）'
);

CREATE INDEX idx_reminder_states_expires ON reminder_states(expires_at);
