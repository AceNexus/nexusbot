-- 創建時區輸入狀態表
-- 用於追蹤正在設定時區的聊天室
CREATE TABLE timezone_input_states (
    room_id VARCHAR(100) PRIMARY KEY,
    resolved_timezone VARCHAR(50),
    original_input VARCHAR(100),
    created_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_timezone_input_states_expires_at ON timezone_input_states(expires_at);
