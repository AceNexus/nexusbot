-- V20: 建立 timezone_input_states 資料表
--
-- 目的：追蹤正在進行時區設定的聊天室狀態，支援多實例環境下的時區設定流程管理

CREATE TABLE timezone_input_states (
    room_id VARCHAR(100) PRIMARY KEY COMMENT '聊天室 ID',
    resolved_timezone VARCHAR(50) COMMENT '解析後的時區名稱（如：Asia/Taipei）',
    original_input VARCHAR(100) COMMENT '用戶原始輸入的時區字串',
    created_at TIMESTAMP COMMENT '建立時間',
    expires_at TIMESTAMP NOT NULL COMMENT '過期時間（30 分鐘後自動清除）'
);

CREATE INDEX idx_timezone_input_states_expires_at ON timezone_input_states(expires_at);
