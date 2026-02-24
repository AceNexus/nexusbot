-- V10: 新增用戶回應欄位至 reminder_logs 資料表
--
-- 目的：追蹤用戶是否確認已執行提醒，支援提醒回應統計與分析

ALTER TABLE reminder_logs
ADD COLUMN user_response_time TIMESTAMP NULL COMMENT '用戶確認回應的時間，NULL 表示尚未回應';

ALTER TABLE reminder_logs
ADD COLUMN user_response_status VARCHAR(20) NULL COMMENT '用戶回應狀態（如：CONFIRMED, SNOOZED）';

CREATE INDEX idx_reminder_logs_response ON reminder_logs(user_response_status, user_response_time);
