-- 創建聊天室表 (H2/MySQL 相容)
CREATE TABLE chat_rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id VARCHAR(100) NOT NULL UNIQUE,
    room_type VARCHAR(10) NOT NULL CHECK (room_type IN ('USER', 'GROUP')),
    ai_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 創建索引提升查詢效能
CREATE INDEX idx_chat_rooms_room_id ON chat_rooms(room_id);
CREATE INDEX idx_chat_rooms_room_type ON chat_rooms(room_type);
CREATE INDEX idx_chat_rooms_ai_enabled ON chat_rooms(ai_enabled);