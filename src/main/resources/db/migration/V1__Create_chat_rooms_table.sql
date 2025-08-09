-- 創建聊天室表
CREATE TABLE chat_rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,                                                -- 主鍵，自動遞增
    room_id VARCHAR(100) NOT NULL UNIQUE,                                                -- 聊天室 ID（LINE userId 或 groupId）
    room_type VARCHAR(10) NOT NULL CHECK (room_type IN ('USER', 'GROUP')),               -- 聊天室類型：USER=個人聊天，GROUP=群組聊天
    ai_enabled BOOLEAN NOT NULL DEFAULT FALSE,                                           -- AI 回應功能是否啟用，預設關閉
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 建立時間
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  -- 更新時間
);

CREATE INDEX idx_chat_rooms_room_id ON chat_rooms(room_id);        -- 聊天室 ID 索引（最常用查詢）
CREATE INDEX idx_chat_rooms_room_type ON chat_rooms(room_type);    -- 聊天室類型索引
CREATE INDEX idx_chat_rooms_ai_enabled ON chat_rooms(ai_enabled);  -- AI 啟用狀態索引