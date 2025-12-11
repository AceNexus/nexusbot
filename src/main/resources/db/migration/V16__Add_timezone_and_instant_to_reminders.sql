-- V16__Add_timezone_and_instant_to_reminders.sql
-- 新增 Reminder 時區與 Instant 欄位

-- 1. 新增 timezone 欄位
ALTER TABLE reminders
ADD COLUMN timezone VARCHAR(50) DEFAULT 'Asia/Taipei';

-- 2. 新增 reminder_time_instant 欄位（允許 NULL，暫時）
ALTER TABLE reminders
ADD COLUMN reminder_time_instant BIGINT;

-- 3. 建立索引（排程器查詢用）
CREATE INDEX idx_reminders_time_instant_status
ON reminders(reminder_time_instant, status);

-- 注意：
-- 1. 現有資料的 reminder_time_instant 為 NULL，將由應用程式啟動時遷移 D:\java\tata\nexusbot\src\main\java\db\migration\V18__Migrate_reminder_time_to_instant.java
-- 2. 保留原本的 reminder_time 欄位
-- 3. 新建立的 Reminder 會直接設定 reminder_time_instant
