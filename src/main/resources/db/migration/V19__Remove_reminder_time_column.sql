-- V19: 移除已棄用的 reminder_time 欄位
--
-- 目的：完成時區安全遷移，移除 reminders 表中已被 reminder_time_instant + timezone 取代的欄位
--
-- 前置條件：
-- 1. V18 已確保所有 Reminder 記錄的 reminder_time_instant 不為 NULL
-- 2. 應用程式已完全改用 Reminder.getLocalTime()（由 instant + timezone 計算本地時間）
-- 3. 已在測試環境驗證無誤

-- 移除依賴 reminder_time 的索引
DROP INDEX IF EXISTS idx_reminders_time_status;

-- 移除已棄用欄位
ALTER TABLE reminders DROP COLUMN IF EXISTS reminder_time;
