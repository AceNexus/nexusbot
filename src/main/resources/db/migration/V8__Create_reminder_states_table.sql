-- 新增提醒狀態表（支援多實體伺服器環境）
-- 用於管理用戶的提醒建立流程狀態

CREATE TABLE reminder_states (
    room_id VARCHAR(100) PRIMARY KEY,              -- 聊天室ID
    step VARCHAR(50) NOT NULL,                     -- 當前步驟：WAITING_FOR_REPEAT_TYPE, WAITING_FOR_TIME, WAITING_FOR_CONTENT
    repeat_type VARCHAR(20),                       -- 重複類型：ONCE, DAILY, WEEKLY
    reminder_time TIMESTAMP,                       -- 暫存的提醒時間
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL                  -- 狀態過期時間（30分鐘後自動清除）
);

-- 索引
CREATE INDEX idx_reminder_states_expires ON reminder_states(expires_at);

-- 註解：此表解決多實體伺服器狀態同步問題
-- 原本使用 ConcurrentHashMap 在單一 JVM 記憶體中，多實體環境會導致狀態不同步
-- 現在改用資料庫存儲，確保所有伺服器實體都能存取相同的狀態資料