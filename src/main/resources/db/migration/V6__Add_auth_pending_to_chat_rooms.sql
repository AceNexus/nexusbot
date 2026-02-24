-- V6: 新增認證等待狀態欄位至 chat_rooms 資料表
--
-- 目的：支援兩步驟認證流程，追蹤聊天室是否正在等待用戶輸入認證密碼

ALTER TABLE chat_rooms
ADD COLUMN auth_pending BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否正在等待用戶輸入認證密碼，預設為 false';
