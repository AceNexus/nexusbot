-- V15__Add_timezone_to_chat_rooms.sql
-- 新增 ChatRoom 時區欄位

-- 新增 timezone 欄位（預設為台灣時區）
ALTER TABLE chat_rooms
ADD COLUMN timezone VARCHAR(50) DEFAULT 'Asia/Taipei';

-- 為現有資料設定預設值
UPDATE chat_rooms
SET timezone = 'Asia/Taipei'
WHERE timezone IS NULL;

-- 建立索引（頻繁查詢時區）
CREATE INDEX idx_chat_rooms_timezone ON chat_rooms(timezone);
