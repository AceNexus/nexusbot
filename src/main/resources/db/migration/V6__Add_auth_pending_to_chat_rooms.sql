-- 目的：支援兩步驟認證流程，新增認證等待狀態欄位
ALTER TABLE chat_rooms 
ADD COLUMN auth_pending BOOLEAN NOT NULL DEFAULT FALSE;