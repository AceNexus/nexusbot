-- 新增用戶回應欄位到 reminder_logs 表
-- 追蹤用戶是否確認已執行提醒

ALTER TABLE reminder_logs 
ADD COLUMN user_response_time TIMESTAMP NULL;

ALTER TABLE reminder_logs 
ADD COLUMN user_response_status VARCHAR(20) NULL;

-- 建立索引以便查詢回應統計
CREATE INDEX idx_reminder_logs_response ON reminder_logs(user_response_status, user_response_time);