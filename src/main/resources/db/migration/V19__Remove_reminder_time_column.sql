-- V19__Remove_reminder_time_column.sql
-- 移除已棄用的 reminder_time 欄位
-- 前提：V18 已確保所有記錄都有 reminder_time_instant

-- ⚠執行前確認：
-- 1. 所有 Reminder 記錄的 reminder_time_instant 都不是 NULL
-- 2. 應用程式已完全改用 getLocalTime() 顯示時間
-- 3. 已在測試環境驗證無誤

-- 先移除索引（因為它依賴於 reminder_time 欄位）
DROP INDEX IF EXISTS idx_reminders_time_status;

-- 移除 reminder_time 欄位
ALTER TABLE reminders DROP COLUMN IF EXISTS reminder_time;

-- 說明：
-- reminder_time 欄位已被 reminder_time_instant + timezone 取代
-- 所有顯示邏輯現在使用 Reminder.getLocalTime() 方法
-- 該方法從 instant + timezone 精確計算本地時間
