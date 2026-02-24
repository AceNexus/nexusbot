-- V13: 建立 email_input_states 資料表
--
-- 目的：支援多實例環境下的 Email 輸入狀態管理
--
-- 設計考量：
-- 1. 原本使用內存 ConcurrentHashMap，多實例環境下無法同步
-- 2. 改用資料庫持久化狀態，確保跨實例一致性
-- 3. 設有 30 分鐘自動過期機制，避免殘留資料累積
-- 4. 使用 room_id 作為主鍵，確保每個聊天室只有一筆進行中的輸入狀態

CREATE TABLE email_input_states (
    room_id VARCHAR(100) PRIMARY KEY COMMENT '聊天室 ID，對應 chat_rooms.room_id',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '狀態建立時間',
    expires_at TIMESTAMP NOT NULL COMMENT '狀態過期時間，過期後會被清理'
);

CREATE INDEX idx_email_input_states_expires_at ON email_input_states(expires_at);
