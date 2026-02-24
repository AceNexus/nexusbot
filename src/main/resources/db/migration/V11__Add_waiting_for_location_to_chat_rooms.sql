-- V11: 新增 waiting_for_location 欄位到 chat_rooms 資料表
--
-- 目的：支援基於位置的廁所搜尋狀態管理
-- 允許系統追蹤用戶何時請求廁所搜尋功能並等待提供位置
-- 避免每次分享位置都觸發不必要的廁所搜尋
--
-- 使用情境：
-- 1. 用戶點擊「找廁所」按鈕 -> 設定 waiting_for_location = true
-- 2. 用戶分享位置 -> 執行廁所搜尋 + 設定 waiting_for_location = false
-- 3. 用戶直接分享位置（未點擊找廁所） -> 僅顯示一般位置確認

ALTER TABLE chat_rooms ADD COLUMN waiting_for_location BOOLEAN NOT NULL DEFAULT FALSE COMMENT '標示聊天室是否正在等待用戶位置以進行廁所搜尋';

-- 新增索引以優化查詢效能（可選但建議）
CREATE INDEX idx_chat_rooms_waiting_for_location ON chat_rooms (waiting_for_location);