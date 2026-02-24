-- V17: 新增時區與 Instant 欄位至 reminder_states 資料表
--
-- 目的：在提醒建立流程中暫存用戶的時區與時間點資訊，與 V16 的 reminders 欄位對齊
--
-- 設計考量：
-- 1. reminder_states 是暫時性狀態儲存，過期後會被清理
-- 2. timezone 與 reminder_instant 用於暫存用戶在建立提醒時輸入的時間資訊

-- 1. 新增時區欄位
ALTER TABLE reminder_states
ADD COLUMN timezone VARCHAR(50) COMMENT '用戶建立提醒時所在時區';

-- 2. 新增 Instant 欄位
ALTER TABLE reminder_states
ADD COLUMN reminder_instant TIMESTAMP COMMENT '暫存的提醒時間點';
