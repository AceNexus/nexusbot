-- V14: 新增提醒通知管道欄位
--
-- 目的：支援 LINE 和 Email 雙管道提醒通知
-- 架構改變：
--   1. reminders 表新增 notification_channel 欄位（預設 LINE）
--   2. reminder_logs 表新增 delivery_method、confirmation_token、confirmed_at 欄位
--   3. reminder_states 表新增 notification_channel 欄位
--
-- 設計考量：
-- 1. 向後相容：預設值為 'LINE'，不影響現有提醒
-- 2. Email 確認機制：使用 UUID token 實作安全的 Email 確認連結
-- 3. 多實例環境：狀態表也需要記錄通知管道選擇

-- 1. reminders 表新增通知管道欄位
ALTER TABLE reminders
ADD COLUMN notification_channel VARCHAR(20) DEFAULT 'LINE';

COMMENT ON COLUMN reminders.notification_channel IS '通知管道：LINE, EMAIL, BOTH';

-- 2. reminder_logs 表新增 Email 確認相關欄位
ALTER TABLE reminder_logs
ADD COLUMN delivery_method VARCHAR(20);

ALTER TABLE reminder_logs
ADD COLUMN confirmation_token VARCHAR(100) UNIQUE;

ALTER TABLE reminder_logs
ADD COLUMN confirmed_at TIMESTAMP NULL;

COMMENT ON COLUMN reminder_logs.delivery_method IS '發送方式：LINE, EMAIL';
COMMENT ON COLUMN reminder_logs.confirmation_token IS 'Email 確認用的 UUID Token';
COMMENT ON COLUMN reminder_logs.confirmed_at IS 'Email 確認時間';

-- 3. reminder_states 表新增通知管道欄位
ALTER TABLE reminder_states
ADD COLUMN notification_channel VARCHAR(20) DEFAULT 'LINE';

COMMENT ON COLUMN reminder_states.notification_channel IS '暫存的通知管道選擇';

-- 4. 建立索引以優化 token 查詢
CREATE INDEX idx_reminder_logs_confirmation_token ON reminder_logs(confirmation_token);
