-- V13: 建立 emails 資料表並移除 chat_rooms 的 email 欄位
--
-- 目的：支援一個聊天室綁定多個信箱的功能
-- 架構改變：從 chat_rooms.email 單一欄位改為獨立的 emails 表（一對多關係）
--
-- 設計考量：
-- 1. 一個聊天室可以有多個信箱
-- 2. 每個信箱可以獨立啟用/停用
-- 3. 使用 room_id 關聯，不使用外鍵約束以提升效能
-- 4. 支援軟刪除（is_active）以保留歷史記錄

-- 建立 emails 資料表
CREATE TABLE emails (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id VARCHAR(100) NOT NULL,                                               -- 聊天室 ID
    email VARCHAR(255) NOT NULL,                                                 -- 電子郵件地址
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,                                    -- 是否啟用此信箱的通知
    is_active BOOLEAN NOT NULL DEFAULT TRUE,                                     -- 是否為啟用狀態（軟刪除用）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,                     -- 建立時間
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  -- 更新時間
);

-- 建立索引
CREATE INDEX idx_emails_room_id ON emails(room_id);
CREATE INDEX idx_emails_is_active ON emails(is_active);
CREATE INDEX idx_emails_room_id_is_active ON emails(room_id, is_active);

-- 移除 chat_rooms 表的 email 相關欄位
ALTER TABLE chat_rooms DROP COLUMN IF EXISTS email;
ALTER TABLE chat_rooms DROP COLUMN IF EXISTS email_enabled;

-- 欄位註解
COMMENT ON COLUMN emails.room_id IS '聊天室 ID，對應 chat_rooms.room_id';
COMMENT ON COLUMN emails.email IS '電子郵件地址';
COMMENT ON COLUMN emails.is_enabled IS '是否啟用此信箱的 Email 通知';
COMMENT ON COLUMN emails.is_active IS '是否為啟用狀態，FALSE 表示已軟刪除';
