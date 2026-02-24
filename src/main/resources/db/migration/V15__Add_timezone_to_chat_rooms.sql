-- V15: 新增時區欄位至 chat_rooms 資料表
--
-- 目的：支援各聊天室獨立設定時區，預設為台灣時區（Asia/Taipei）
-- 確保提醒時間在不同時區環境下正確顯示

ALTER TABLE chat_rooms
ADD COLUMN timezone VARCHAR(50) DEFAULT 'Asia/Taipei' COMMENT '聊天室時區設定，預設為 Asia/Taipei';

UPDATE chat_rooms
SET timezone = 'Asia/Taipei'
WHERE timezone IS NULL;

CREATE INDEX idx_chat_rooms_timezone ON chat_rooms(timezone);
