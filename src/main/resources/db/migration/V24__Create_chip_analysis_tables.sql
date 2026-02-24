-- V24: 建立籌碼分析相關資料表
--
-- 目的：儲存融資融券、借券、外資持股及集保分散等籌碼分析所需資料
--
-- 包含四張表：
-- 1. margin_trading_stats       — 融資融券統計表
-- 2. securities_lending_stats   — 借券賣出統計表
-- 3. foreign_shareholding_stats — 外資持股比例統計表
-- 4. shareholding_distribution  — 集保分散表

-- 1. 融資融券統計表
CREATE TABLE margin_trading_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    stock_symbol VARCHAR(20) NOT NULL COMMENT '股票代號',
    trade_date DATE NOT NULL COMMENT '交易日期',

    margin_purchase_buy BIGINT DEFAULT 0 COMMENT '融資買進',
    margin_purchase_sell BIGINT DEFAULT 0 COMMENT '融資賣出',
    margin_purchase_cash_repayment BIGINT DEFAULT 0 COMMENT '融資現金償還',
    margin_purchase_yesterday_balance BIGINT DEFAULT 0 COMMENT '融資前日餘額',
    margin_purchase_today_balance BIGINT DEFAULT 0 COMMENT '融資今日餘額',
    margin_purchase_limit BIGINT DEFAULT 0 COMMENT '融資限額',
    margin_purchase_change BIGINT DEFAULT 0 COMMENT '融資增減',

    short_sale_buy BIGINT DEFAULT 0 COMMENT '融券買進',
    short_sale_sell BIGINT DEFAULT 0 COMMENT '融券賣出',
    short_sale_cash_repayment BIGINT DEFAULT 0 COMMENT '融券現金償還',
    short_sale_yesterday_balance BIGINT DEFAULT 0 COMMENT '融券前日餘額',
    short_sale_today_balance BIGINT DEFAULT 0 COMMENT '融券今日餘額',
    short_sale_limit BIGINT DEFAULT 0 COMMENT '融券限額',
    short_sale_change BIGINT DEFAULT 0 COMMENT '融券增減',

    offset_loan_and_short BIGINT DEFAULT 0 COMMENT '資券互抵',
    utilization_rate DECIMAL(8,4) DEFAULT 0 COMMENT '資券使用率',
    note VARCHAR(255) COMMENT '備註',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',

    CONSTRAINT uk_margin_stock_date UNIQUE (stock_symbol, trade_date)
);

CREATE INDEX idx_margin_trade_date ON margin_trading_stats(trade_date);
CREATE INDEX idx_margin_stock_symbol ON margin_trading_stats(stock_symbol);

-- 2. 借券賣出統計表（每日彙總）
CREATE TABLE securities_lending_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    stock_symbol VARCHAR(20) NOT NULL COMMENT '股票代號',
    trade_date DATE NOT NULL COMMENT '交易日期',

    transaction_type VARCHAR(20) COMMENT '交易類型',
    total_volume BIGINT DEFAULT 0 COMMENT '總成交量',
    total_transactions INT DEFAULT 0 COMMENT '總筆數',
    avg_fee_rate DECIMAL(8,4) DEFAULT 0 COMMENT '平均費率',
    close_price DECIMAL(12,2) COMMENT '收盤價',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',

    CONSTRAINT uk_lending_stock_date_type UNIQUE (stock_symbol, trade_date, transaction_type)
);

CREATE INDEX idx_lending_trade_date ON securities_lending_stats(trade_date);
CREATE INDEX idx_lending_stock_symbol ON securities_lending_stats(stock_symbol);

-- 3. 外資持股比例統計表
CREATE TABLE foreign_shareholding_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    stock_symbol VARCHAR(20) NOT NULL COMMENT '股票代號',
    stock_name VARCHAR(100) COMMENT '股票名稱',
    trade_date DATE NOT NULL COMMENT '交易日期',

    foreign_investment_shares BIGINT DEFAULT 0 COMMENT '外資持股張數',
    foreign_investment_remaining_shares BIGINT DEFAULT 0 COMMENT '外資可投資剩餘張數',
    foreign_investment_shares_ratio DECIMAL(8,4) DEFAULT 0 COMMENT '外資持股比率（%）',
    foreign_investment_remain_ratio DECIMAL(8,4) DEFAULT 0 COMMENT '外資可投資比率（%）',
    foreign_investment_upper_limit_ratio DECIMAL(8,4) DEFAULT 0 COMMENT '外資投資上限比率（%）',
    number_of_shares_issued BIGINT DEFAULT 0 COMMENT '已發行股數',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',

    CONSTRAINT uk_foreign_stock_date UNIQUE (stock_symbol, trade_date)
);

CREATE INDEX idx_foreign_trade_date ON foreign_shareholding_stats(trade_date);
CREATE INDEX idx_foreign_stock_symbol ON foreign_shareholding_stats(stock_symbol);

-- 4. 集保分散表
CREATE TABLE shareholding_distribution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    stock_symbol VARCHAR(20) NOT NULL COMMENT '股票代號',
    data_date DATE NOT NULL COMMENT '資料日期',
    holding_shares_level VARCHAR(50) NOT NULL COMMENT '持股分級（如：1-999, 1000-5000）',

    people BIGINT DEFAULT 0 COMMENT '持股人數',
    percent DECIMAL(8,4) DEFAULT 0 COMMENT '持股比率（%）',
    unit BIGINT DEFAULT 0 COMMENT '持股張數',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',

    CONSTRAINT uk_distribution_stock_date_level UNIQUE (stock_symbol, data_date, holding_shares_level)
);

CREATE INDEX idx_distribution_data_date ON shareholding_distribution(data_date);
CREATE INDEX idx_distribution_stock_symbol ON shareholding_distribution(stock_symbol);
