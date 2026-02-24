-- V1: 建立 chat_rooms 資料表
--
-- 目的：儲存 LINE Bot 聊天室基本資訊，支援個人與群組聊天室的 AI 功能管理
--
-- 設計考量：
-- 1. room_id 使用 LINE 提供的 userId 或 groupId
-- 2. 以 room_type 區分個人（USER）與群組（GROUP）聊天室
-- 3. ai_enabled 欄位控制各聊天室的 AI 回應功能開關

CREATE TABLE chat_rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    room_id VARCHAR(100) NOT NULL UNIQUE COMMENT '聊天室 ID（LINE userId 或 groupId）',
    room_type VARCHAR(10) NOT NULL CHECK (room_type IN ('USER', 'GROUP')) COMMENT '聊天室類型：USER=個人聊天，GROUP=群組聊天',
    ai_enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'AI 回應功能是否啟用，預設關閉',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間'
);

CREATE INDEX idx_chat_rooms_room_id ON chat_rooms(room_id);
CREATE INDEX idx_chat_rooms_room_type ON chat_rooms(room_type);
CREATE INDEX idx_chat_rooms_ai_enabled ON chat_rooms(ai_enabled);
