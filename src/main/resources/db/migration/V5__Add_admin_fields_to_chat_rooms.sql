-- 目的：支援管理員身份識別，新增管理員認證欄位到聊天室表
ALTER TABLE chat_rooms 
ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- 為管理員相關欄位建立索引，提升查詢效能
CREATE INDEX idx_chat_rooms_is_admin ON chat_rooms(is_admin);