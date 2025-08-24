-- 在 chat_rooms 表中添加 ai_model 欄位，支援模型選擇功能
ALTER TABLE chat_rooms ADD COLUMN ai_model VARCHAR(50) DEFAULT 'llama-3.1-8b-instant';

-- 為 ai_model 創建索引，提升查詢效能
CREATE INDEX idx_chat_rooms_ai_model ON chat_rooms(ai_model);

-- 更新現有記錄的預設模型（如果有的話）
UPDATE chat_rooms SET ai_model = 'llama-3.1-8b-instant' WHERE ai_model IS NULL;