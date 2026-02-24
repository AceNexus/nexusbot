-- V5: 新增管理員欄位至 chat_rooms 資料表
--
-- 目的：支援管理員身份識別，區分一般用戶與管理員聊天室以提供進階管理功能

ALTER TABLE chat_rooms
ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否為管理員聊天室，預設為 false';

CREATE INDEX idx_chat_rooms_is_admin ON chat_rooms(is_admin);
