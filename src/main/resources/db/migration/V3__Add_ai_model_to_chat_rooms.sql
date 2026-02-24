-- V3: 新增 ai_model 欄位至 chat_rooms 資料表
--
-- 目的：支援每個聊天室獨立選擇 AI 模型，預設使用 llama-3.1-8b-instant

ALTER TABLE chat_rooms ADD COLUMN ai_model VARCHAR(50) DEFAULT 'llama-3.1-8b-instant' COMMENT '使用的 AI 模型名稱（如：llama-3.1-8b-instant）';

CREATE INDEX idx_chat_rooms_ai_model ON chat_rooms(ai_model);

UPDATE chat_rooms SET ai_model = 'llama-3.1-8b-instant' WHERE ai_model IS NULL;
