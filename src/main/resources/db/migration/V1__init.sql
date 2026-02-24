-- ==========================================
-- NexusBot 完整資料庫 Schema 初始化
-- 整合自 V1~V24 所有 migration，代表最終 schema 狀態
-- ==========================================


-- ==========================================
-- 1. chat_rooms：聊天室基本資訊
-- ==========================================
CREATE TABLE chat_rooms (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    room_id              VARCHAR(100) NOT NULL UNIQUE COMMENT '聊天室 ID（LINE userId 或 groupId）',
    room_type            VARCHAR(10)  NOT NULL CHECK (room_type IN ('USER', 'GROUP')) COMMENT '聊天室類型：USER=個人，GROUP=群組',
    ai_enabled           BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'AI 回應功能開關',
    ai_model             VARCHAR(50)           DEFAULT 'llama-3.1-8b-instant' COMMENT '使用的 AI 模型名稱',
    is_admin             BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '是否為管理員聊天室',
    auth_pending         BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '是否正在等待輸入認證密碼',
    waiting_for_location BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '是否正在等待用戶位置以進行廁所搜尋',
    timezone             VARCHAR(50)           DEFAULT 'Asia/Taipei' COMMENT '聊天室時區設定',
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間'
);

CREATE INDEX idx_chat_rooms_room_id              ON chat_rooms(room_id);
CREATE INDEX idx_chat_rooms_room_type            ON chat_rooms(room_type);
CREATE INDEX idx_chat_rooms_ai_enabled           ON chat_rooms(ai_enabled);
CREATE INDEX idx_chat_rooms_ai_model             ON chat_rooms(ai_model);
CREATE INDEX idx_chat_rooms_is_admin             ON chat_rooms(is_admin);
CREATE INDEX idx_chat_rooms_waiting_for_location ON chat_rooms(waiting_for_location);
CREATE INDEX idx_chat_rooms_timezone             ON chat_rooms(timezone);


-- ==========================================
-- 2. chat_messages：AI 對話記錄
-- ==========================================
CREATE TABLE chat_messages (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    room_id           VARCHAR(100) NOT NULL COMMENT '聊天室 ID',
    room_type         VARCHAR(10)  NOT NULL CHECK (room_type IN ('USER', 'GROUP')) COMMENT '聊天室類型（冗余設計）',
    user_id           VARCHAR(100)          COMMENT '發送者 LINE userId，AI 訊息為 NULL',
    message_type      VARCHAR(20)  NOT NULL CHECK (message_type IN ('USER', 'AI')) COMMENT '訊息類型',
    content           TEXT         NOT NULL COMMENT '訊息內容',
    tokens_used       INTEGER               DEFAULT 0 COMMENT 'AI 使用 token 數（成本統計）',
    processing_time_ms INTEGER              DEFAULT 0 COMMENT 'AI 處理時間（毫秒）',
    ai_model          VARCHAR(50)           COMMENT '使用的 AI 模型名稱',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    deleted_at        TIMESTAMP             NULL COMMENT '軟刪除時間，NULL 表示未刪除'
);

CREATE INDEX idx_chat_messages_room_id         ON chat_messages(room_id);
CREATE INDEX idx_chat_messages_user_id         ON chat_messages(user_id);
CREATE INDEX idx_chat_messages_room_type       ON chat_messages(room_type);
CREATE INDEX idx_chat_messages_message_type    ON chat_messages(message_type);
CREATE INDEX idx_chat_messages_created_at      ON chat_messages(created_at);
CREATE INDEX idx_chat_messages_deleted_at      ON chat_messages(deleted_at);
CREATE INDEX idx_chat_messages_room_created    ON chat_messages(room_id, created_at);
CREATE INDEX idx_chat_messages_room_not_deleted ON chat_messages(room_id, deleted_at);
CREATE INDEX idx_chat_messages_room_user_time  ON chat_messages(room_id, user_id, created_at DESC);


-- ==========================================
-- 3. reminders：提醒設定
-- ==========================================
CREATE TABLE reminders (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '提醒 ID，自動遞增',
    room_id               VARCHAR(100) NOT NULL COMMENT '聊天室 ID',
    content               TEXT         NOT NULL COMMENT '提醒內容',
    timezone              VARCHAR(50)           DEFAULT 'Asia/Taipei' COMMENT '建立提醒時的時區',
    reminder_time_instant BIGINT                COMMENT '提醒時間點（epoch millis）',
    repeat_type           VARCHAR(20)           DEFAULT 'ONCE' COMMENT '重複類型：ONCE, DAILY, WEEKLY',
    status                VARCHAR(20)           DEFAULT 'ACTIVE' COMMENT '狀態：ACTIVE, PAUSED, COMPLETED',
    created_by            VARCHAR(100) NOT NULL COMMENT '建立者（LINE userId）',
    notification_channel  VARCHAR(20)           DEFAULT 'LINE' COMMENT '通知管道：LINE, EMAIL, BOTH',
    created_at            TIMESTAMP             DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間'
);

CREATE INDEX idx_reminders_room                ON reminders(room_id);
CREATE INDEX idx_reminders_time_instant_status ON reminders(reminder_time_instant, status);


-- ==========================================
-- 4. reminder_logs：提醒發送記錄
-- ==========================================
CREATE TABLE reminder_logs (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '記錄 ID，自動遞增',
    reminder_id          BIGINT       NOT NULL COMMENT '提醒 ID',
    room_id              VARCHAR(100) NOT NULL COMMENT '聊天室 ID',
    sent_time            TIMESTAMP    NOT NULL COMMENT '發送時間',
    status               VARCHAR(20)  NOT NULL COMMENT '發送狀態：SENT, FAILED',
    error_message        TEXT                  COMMENT '錯誤訊息（發送失敗時記錄）',
    user_response_time   TIMESTAMP             NULL COMMENT '用戶確認回應時間',
    user_response_status VARCHAR(20)           NULL COMMENT '用戶回應狀態（CONFIRMED, SNOOZED）',
    delivery_method      VARCHAR(20)           COMMENT '發送方式：LINE, EMAIL',
    confirmation_token   VARCHAR(100)          UNIQUE COMMENT 'Email 確認用的 UUID Token',
    confirmed_at         TIMESTAMP             NULL COMMENT 'Email 確認時間',
    created_at           TIMESTAMP             DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間'
);

CREATE INDEX idx_logs_reminder                     ON reminder_logs(reminder_id);
CREATE INDEX idx_logs_room                         ON reminder_logs(room_id);
CREATE INDEX idx_reminder_logs_response            ON reminder_logs(user_response_status, user_response_time);
CREATE INDEX idx_reminder_logs_confirmation_token  ON reminder_logs(confirmation_token);


-- ==========================================
-- 5. reminder_locks：分散式鎖定（防重複發送）
-- ==========================================
CREATE TABLE reminder_locks (
    lock_key  VARCHAR(100) PRIMARY KEY COMMENT '鎖定鍵（唯一，防止重複發送）',
    locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '鎖定時間'
);

CREATE INDEX idx_locks_locked_at ON reminder_locks(locked_at);


-- ==========================================
-- 6. reminder_states：提醒建立流程暫存狀態
-- ==========================================
CREATE TABLE reminder_states (
    room_id              VARCHAR(100) PRIMARY KEY COMMENT '聊天室 ID',
    step                 VARCHAR(50)  NOT NULL COMMENT '當前步驟：WAITING_FOR_REPEAT_TYPE, WAITING_FOR_TIME, WAITING_FOR_CONTENT',
    repeat_type          VARCHAR(20)           COMMENT '暫存的重複類型',
    reminder_time        TIMESTAMP             COMMENT '暫存的提醒時間',
    timezone             VARCHAR(50)           COMMENT '暫存的時區',
    reminder_instant     TIMESTAMP             COMMENT '暫存的提醒時間點',
    notification_channel VARCHAR(20)           DEFAULT 'LINE' COMMENT '暫存的通知管道選擇',
    created_at           TIMESTAMP             DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    expires_at           TIMESTAMP    NOT NULL COMMENT '狀態過期時間（30 分鐘後自動清除）'
);

CREATE INDEX idx_reminder_states_expires ON reminder_states(expires_at);


-- ==========================================
-- 7. emails：聊天室綁定信箱（一對多）
-- ==========================================
CREATE TABLE emails (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    room_id    VARCHAR(100) NOT NULL COMMENT '聊天室 ID',
    email      VARCHAR(255) NOT NULL COMMENT '電子郵件地址',
    is_enabled BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '是否啟用此信箱的 Email 通知',
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '是否啟用，FALSE 表示已軟刪除',
    created_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at TIMESTAMP             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間'
);

CREATE INDEX idx_emails_room_id            ON emails(room_id);
CREATE INDEX idx_emails_is_active          ON emails(is_active);
CREATE INDEX idx_emails_room_id_is_active  ON emails(room_id, is_active);


-- ==========================================
-- 8. email_input_states：Email 輸入流程暫存狀態
-- ==========================================
CREATE TABLE email_input_states (
    room_id    VARCHAR(100) PRIMARY KEY COMMENT '聊天室 ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '狀態建立時間',
    expires_at TIMESTAMP NOT NULL COMMENT '狀態過期時間'
);

CREATE INDEX idx_email_input_states_expires_at ON email_input_states(expires_at);


-- ==========================================
-- 9. timezone_input_states：時區設定流程暫存狀態
-- ==========================================
CREATE TABLE timezone_input_states (
    room_id            VARCHAR(100) PRIMARY KEY COMMENT '聊天室 ID',
    resolved_timezone  VARCHAR(50)           COMMENT '解析後的時區名稱（如：Asia/Taipei）',
    original_input     VARCHAR(100)          COMMENT '用戶原始輸入',
    created_at         TIMESTAMP             COMMENT '建立時間',
    expires_at         TIMESTAMP    NOT NULL COMMENT '過期時間'
);

CREATE INDEX idx_timezone_input_states_expires_at ON timezone_input_states(expires_at);


-- ==========================================
-- 10. stock_groups：股票群組定義
-- ==========================================
CREATE TABLE stock_groups (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '群組 ID，自動遞增',
    user_id       VARCHAR(100) NOT NULL COMMENT 'LINE userId',
    name          VARCHAR(100) NOT NULL COMMENT '群組名稱',
    display_order INT          NOT NULL DEFAULT 0 COMMENT '顯示排序',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '是否啟用，FALSE 表示已軟刪除',
    is_selected   BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '是否為目前選取的群組',
    created_at    TIMESTAMP             DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at    TIMESTAMP             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間'
);

CREATE INDEX idx_stock_groups_user_id     ON stock_groups(user_id);
CREATE INDEX idx_stock_groups_user_active ON stock_groups(user_id, is_active);


-- ==========================================
-- 11. stock_group_items：群組內股票明細
-- ==========================================
CREATE TABLE stock_group_items (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '明細 ID，自動遞增',
    group_id      BIGINT       NOT NULL COMMENT '所屬群組 ID',
    stock_symbol  VARCHAR(20)  NOT NULL COMMENT '股票代號（如：2330）',
    stock_name    VARCHAR(100)          COMMENT '股票名稱（如：台積電）',
    market        VARCHAR(10)           COMMENT '股票所屬市場（如：TSE, OTC）',
    display_order INT          NOT NULL DEFAULT 0 COMMENT '群組內顯示排序',
    created_at    TIMESTAMP             DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間'
);

CREATE INDEX idx_stock_group_items_group   ON stock_group_items(group_id);
CREATE UNIQUE INDEX idx_stock_group_items_unique ON stock_group_items(group_id, stock_symbol);


-- ==========================================
-- 12. institutional_investor_stats：三大法人統計
-- ==========================================
CREATE TABLE institutional_investor_stats (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    stock_symbol              VARCHAR(20)  NOT NULL COMMENT '股票代號',
    stock_name                VARCHAR(100)          COMMENT '股票名稱',
    trade_date                DATE         NOT NULL COMMENT '交易日期',
    foreign_investor_buy      BIGINT                DEFAULT 0 COMMENT '外資買進張數',
    foreign_investor_sell     BIGINT                DEFAULT 0 COMMENT '外資賣出張數',
    foreign_investor_buy_sell BIGINT                DEFAULT 0 COMMENT '外資買賣超張數',
    investment_trust_buy      BIGINT                DEFAULT 0 COMMENT '投信買進張數',
    investment_trust_sell     BIGINT                DEFAULT 0 COMMENT '投信賣出張數',
    investment_trust_buy_sell BIGINT                DEFAULT 0 COMMENT '投信買賣超張數',
    dealer_buy                BIGINT                DEFAULT 0 COMMENT '自營商買進張數',
    dealer_sell               BIGINT                DEFAULT 0 COMMENT '自營商賣出張數',
    dealer_buy_sell           BIGINT                DEFAULT 0 COMMENT '自營商買賣超張數',
    total_buy                 BIGINT                DEFAULT 0 COMMENT '三大法人合計買進張數',
    total_sell                BIGINT                DEFAULT 0 COMMENT '三大法人合計賣出張數',
    total_buy_sell            BIGINT                DEFAULT 0 COMMENT '三大法人合計買賣超張數',
    created_at                TIMESTAMP             DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    CONSTRAINT uk_stock_date UNIQUE (stock_symbol, trade_date)
);

CREATE INDEX idx_trade_date   ON institutional_investor_stats(trade_date);
CREATE INDEX idx_stock_symbol ON institutional_investor_stats(stock_symbol);


-- ==========================================
-- 13. margin_trading_stats：融資融券統計
-- ==========================================
CREATE TABLE margin_trading_stats (
    id                                 BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    stock_symbol                       VARCHAR(20)    NOT NULL COMMENT '股票代號',
    trade_date                         DATE           NOT NULL COMMENT '交易日期',
    margin_purchase_buy                BIGINT                  DEFAULT 0 COMMENT '融資買進',
    margin_purchase_sell               BIGINT                  DEFAULT 0 COMMENT '融資賣出',
    margin_purchase_cash_repayment     BIGINT                  DEFAULT 0 COMMENT '融資現金償還',
    margin_purchase_yesterday_balance  BIGINT                  DEFAULT 0 COMMENT '融資前日餘額',
    margin_purchase_today_balance      BIGINT                  DEFAULT 0 COMMENT '融資今日餘額',
    margin_purchase_limit              BIGINT                  DEFAULT 0 COMMENT '融資限額',
    margin_purchase_change             BIGINT                  DEFAULT 0 COMMENT '融資增減',
    short_sale_buy                     BIGINT                  DEFAULT 0 COMMENT '融券買進',
    short_sale_sell                    BIGINT                  DEFAULT 0 COMMENT '融券賣出',
    short_sale_cash_repayment          BIGINT                  DEFAULT 0 COMMENT '融券現金償還',
    short_sale_yesterday_balance       BIGINT                  DEFAULT 0 COMMENT '融券前日餘額',
    short_sale_today_balance           BIGINT                  DEFAULT 0 COMMENT '融券今日餘額',
    short_sale_limit                   BIGINT                  DEFAULT 0 COMMENT '融券限額',
    short_sale_change                  BIGINT                  DEFAULT 0 COMMENT '融券增減',
    offset_loan_and_short              BIGINT                  DEFAULT 0 COMMENT '資券互抵',
    utilization_rate                   DECIMAL(8, 4)           DEFAULT 0 COMMENT '資券使用率',
    note                               VARCHAR(255)            COMMENT '備註',
    created_at                         TIMESTAMP               DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    CONSTRAINT uk_margin_stock_date UNIQUE (stock_symbol, trade_date)
);

CREATE INDEX idx_margin_trade_date   ON margin_trading_stats(trade_date);
CREATE INDEX idx_margin_stock_symbol ON margin_trading_stats(stock_symbol);


-- ==========================================
-- 14. securities_lending_stats：借券賣出統計
-- ==========================================
CREATE TABLE securities_lending_stats (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    stock_symbol        VARCHAR(20)    NOT NULL COMMENT '股票代號',
    trade_date          DATE           NOT NULL COMMENT '交易日期',
    transaction_type    VARCHAR(20)             COMMENT '交易類型',
    total_volume        BIGINT                  DEFAULT 0 COMMENT '總成交量',
    total_transactions  INT                     DEFAULT 0 COMMENT '總筆數',
    avg_fee_rate        DECIMAL(8, 4)           DEFAULT 0 COMMENT '平均費率',
    close_price         DECIMAL(12, 2)          COMMENT '收盤價',
    created_at          TIMESTAMP               DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    CONSTRAINT uk_lending_stock_date_type UNIQUE (stock_symbol, trade_date, transaction_type)
);

CREATE INDEX idx_lending_trade_date   ON securities_lending_stats(trade_date);
CREATE INDEX idx_lending_stock_symbol ON securities_lending_stats(stock_symbol);


-- ==========================================
-- 15. foreign_shareholding_stats：外資持股比例統計
-- ==========================================
CREATE TABLE foreign_shareholding_stats (
    id                                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    stock_symbol                        VARCHAR(20)    NOT NULL COMMENT '股票代號',
    stock_name                          VARCHAR(100)            COMMENT '股票名稱',
    trade_date                          DATE           NOT NULL COMMENT '交易日期',
    foreign_investment_shares           BIGINT                  DEFAULT 0 COMMENT '外資持股張數',
    foreign_investment_remaining_shares BIGINT                  DEFAULT 0 COMMENT '外資可投資剩餘張數',
    foreign_investment_shares_ratio     DECIMAL(8, 4)           DEFAULT 0 COMMENT '外資持股比率（%）',
    foreign_investment_remain_ratio     DECIMAL(8, 4)           DEFAULT 0 COMMENT '外資可投資比率（%）',
    foreign_investment_upper_limit_ratio DECIMAL(8, 4)          DEFAULT 0 COMMENT '外資投資上限比率（%）',
    number_of_shares_issued             BIGINT                  DEFAULT 0 COMMENT '已發行股數',
    created_at                          TIMESTAMP               DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    CONSTRAINT uk_foreign_stock_date UNIQUE (stock_symbol, trade_date)
);

CREATE INDEX idx_foreign_trade_date   ON foreign_shareholding_stats(trade_date);
CREATE INDEX idx_foreign_stock_symbol ON foreign_shareholding_stats(stock_symbol);


-- ==========================================
-- 16. shareholding_distribution：集保分散表
-- ==========================================
CREATE TABLE shareholding_distribution (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    stock_symbol         VARCHAR(20)    NOT NULL COMMENT '股票代號',
    data_date            DATE           NOT NULL COMMENT '資料日期',
    holding_shares_level VARCHAR(50)    NOT NULL COMMENT '持股分級（如：1-999, 1000-5000）',
    people               BIGINT                  DEFAULT 0 COMMENT '持股人數',
    percent              DECIMAL(8, 4)           DEFAULT 0 COMMENT '持股比率（%）',
    unit                 BIGINT                  DEFAULT 0 COMMENT '持股張數',
    created_at           TIMESTAMP               DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    CONSTRAINT uk_distribution_stock_date_level UNIQUE (stock_symbol, data_date, holding_shares_level)
);

CREATE INDEX idx_distribution_data_date    ON shareholding_distribution(data_date);
CREATE INDEX idx_distribution_stock_symbol ON shareholding_distribution(stock_symbol);
