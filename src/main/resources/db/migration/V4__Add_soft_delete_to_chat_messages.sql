-- V4: 新增軟刪除欄位至 chat_messages 資料表
--
-- 目的：支援歷史對話紀錄軟刪除，保留資料以供稽核，透過 deleted_at 標記刪除狀態

ALTER TABLE chat_messages ADD COLUMN deleted_at TIMESTAMP NULL COMMENT '軟刪除時間，NULL 表示未刪除';

-- 新增索引以優化排除已刪除訊息的查詢
CREATE INDEX idx_chat_messages_deleted_at ON chat_messages(deleted_at);

-- 新增複合索引以優化按聊天室查詢未刪除訊息
CREATE INDEX idx_chat_messages_room_not_deleted ON chat_messages(room_id, deleted_at);
