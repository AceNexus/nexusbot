-- V17__Add_timezone_and_instant_to_reminder_states.sql
-- 新增 ReminderState 時區與 Instant 欄位

-- 1. 新增 timezone 欄位
ALTER TABLE reminder_states
ADD COLUMN timezone VARCHAR(50);

-- 2. 新增 reminder_instant 欄位
ALTER TABLE reminder_states
ADD COLUMN reminder_instant TIMESTAMP;

-- 注意：
-- 1. 這些欄位用於暫存使用者在建立提醒時的時間資訊
-- 2. ReminderState 是暫時性的狀態儲存，過期後會被清理
