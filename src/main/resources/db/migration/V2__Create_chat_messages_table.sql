-- V2: 建立 chat_messages 資料表
--
-- 目的：記錄用戶與 AI 的完整對話流程，支援多輪對話追蹤和成本統計
--
-- 設計考量：
-- 1. 與 chat_rooms 表建立 1:N 關係，但不使用外鍵約束
-- 2. 採用應用層控制一致性，避免高併發寫入的性能問題
-- 3. 冗余 room_type 欄位，提升查詢效能並提供資料一致性檢查
-- 4. 支援 AI 對話成本統計和效能監控

CREATE TABLE chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    room_id VARCHAR(100) NOT NULL COMMENT '聊天室 ID（LINE userId 或 groupId）',
    room_type VARCHAR(10) NOT NULL CHECK (room_type IN ('USER', 'GROUP')) COMMENT '聊天室類型：USER=個人聊天，GROUP=群組聊天（冗余設計）',
    user_id VARCHAR(100) COMMENT '發送者 ID（LINE userId），AI 訊息此欄為 null',
    message_type VARCHAR(20) NOT NULL CHECK (message_type IN ('USER', 'AI')) COMMENT '訊息類型：USER=用戶訊息，AI=AI 回應',
    content TEXT NOT NULL COMMENT '訊息內容（支援長文本）',
    tokens_used INTEGER DEFAULT 0 COMMENT 'AI 處理使用的 tokens 數量（成本統計用）',
    processing_time_ms INTEGER DEFAULT 0 COMMENT 'AI 處理時間（毫秒，效能監控用）',
    ai_model VARCHAR(50) COMMENT '使用的 AI 模型名稱（如：llama-3.1-8b-instant）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間'
);

CREATE INDEX idx_chat_messages_room_id ON chat_messages(room_id);
CREATE INDEX idx_chat_messages_user_id ON chat_messages(user_id);
CREATE INDEX idx_chat_messages_room_type ON chat_messages(room_type);
CREATE INDEX idx_chat_messages_message_type ON chat_messages(message_type);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at);
CREATE INDEX idx_chat_messages_room_created ON chat_messages(room_id, created_at);
CREATE INDEX idx_chat_messages_room_user_time ON chat_messages(room_id, user_id, created_at DESC);
