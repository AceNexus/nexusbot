-- V16: 新增時區與 Instant 欄位至 reminders 資料表
--
-- 目的：以時區安全的方式儲存提醒時間，將時間點改以 epoch millis（BIGINT）儲存
--
-- 設計考量：
-- 1. timezone 欄位記錄建立提醒時的時區
-- 2. reminder_time_instant 以 epoch millis 儲存，避免時區轉換問題
-- 3. 保留原有 reminder_time 欄位，待 V18 資料遷移完成後由 V19 移除

-- 1. 新增時區欄位
ALTER TABLE reminders
ADD COLUMN timezone VARCHAR(50) DEFAULT 'Asia/Taipei' COMMENT '建立提醒時的時區（如：Asia/Taipei）';

-- 2. 新增 Instant 欄位（允許 NULL，由 V18 遷移既有資料）
ALTER TABLE reminders
ADD COLUMN reminder_time_instant BIGINT COMMENT '提醒時間點（epoch millis），取代 reminder_time 欄位';

-- 3. 建立索引供排程器查詢
CREATE INDEX idx_reminders_time_instant_status
ON reminders(reminder_time_instant, status);
