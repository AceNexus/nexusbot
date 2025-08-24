-- 為 chat_messages 表新增軟刪除功能，新增 deleted_at 欄位支援歷史對話紀錄軟刪除
ALTER TABLE chat_messages ADD COLUMN deleted_at TIMESTAMP NULL;

-- 創建索引以優化查詢效能（排除已刪除的訊息）
CREATE INDEX idx_chat_messages_deleted_at ON chat_messages(deleted_at);

-- 創建複合索引以優化按房間查詢未刪除訊息
CREATE INDEX idx_chat_messages_room_not_deleted ON chat_messages(room_id, deleted_at);