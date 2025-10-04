-- V13: 建立 email_input_states 資料表
--
-- 目的：支援多實例環境下的 Email 輸入狀態管理
-- 架構改變：從內存 ConcurrentHashMap 改為資料庫持久化狀態
--
-- 設計考量：
-- 1. 多實例環境下狀態同步
-- 2. 自動過期機制（30分鐘後清除）
-- 3. 使用 room_id 作為主鍵，確保唯一性

-- 建立 email_input_states 資料表
CREATE TABLE email_input_states (
    room_id VARCHAR(100) PRIMARY KEY,                                    -- 聊天室 ID
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,                      -- 建立時間
    expires_at TIMESTAMP NOT NULL                                        -- 過期時間（30分鐘後）
);

-- 建立索引以優化過期記錄查詢
CREATE INDEX idx_email_input_states_expires_at ON email_input_states(expires_at);

-- 欄位註解
COMMENT ON COLUMN email_input_states.room_id IS '聊天室 ID，對應 chat_rooms.room_id';
COMMENT ON COLUMN email_input_states.created_at IS '狀態建立時間';
COMMENT ON COLUMN email_input_states.expires_at IS '狀態過期時間，過期後會被清理';
