-- 融資融券統計
CREATE TABLE margin_trading_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_symbol VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,

    margin_purchase_buy BIGINT DEFAULT 0,
    margin_purchase_sell BIGINT DEFAULT 0,
    margin_purchase_cash_repayment BIGINT DEFAULT 0,
    margin_purchase_yesterday_balance BIGINT DEFAULT 0,
    margin_purchase_today_balance BIGINT DEFAULT 0,
    margin_purchase_limit BIGINT DEFAULT 0,
    margin_purchase_change BIGINT DEFAULT 0,

    short_sale_buy BIGINT DEFAULT 0,
    short_sale_sell BIGINT DEFAULT 0,
    short_sale_cash_repayment BIGINT DEFAULT 0,
    short_sale_yesterday_balance BIGINT DEFAULT 0,
    short_sale_today_balance BIGINT DEFAULT 0,
    short_sale_limit BIGINT DEFAULT 0,
    short_sale_change BIGINT DEFAULT 0,

    offset_loan_and_short BIGINT DEFAULT 0,
    utilization_rate DECIMAL(8,4) DEFAULT 0,
    note VARCHAR(255),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_margin_stock_date UNIQUE (stock_symbol, trade_date)
);

CREATE INDEX idx_margin_trade_date ON margin_trading_stats(trade_date);
CREATE INDEX idx_margin_stock_symbol ON margin_trading_stats(stock_symbol);

-- 借券賣出統計（每日彙總）
CREATE TABLE securities_lending_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_symbol VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,

    transaction_type VARCHAR(20),
    total_volume BIGINT DEFAULT 0,
    total_transactions INT DEFAULT 0,
    avg_fee_rate DECIMAL(8,4) DEFAULT 0,
    close_price DECIMAL(12,2),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_lending_stock_date_type UNIQUE (stock_symbol, trade_date, transaction_type)
);

CREATE INDEX idx_lending_trade_date ON securities_lending_stats(trade_date);
CREATE INDEX idx_lending_stock_symbol ON securities_lending_stats(stock_symbol);

-- 外資持股比例統計
CREATE TABLE foreign_shareholding_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_symbol VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100),
    trade_date DATE NOT NULL,

    foreign_investment_shares BIGINT DEFAULT 0,
    foreign_investment_remaining_shares BIGINT DEFAULT 0,
    foreign_investment_shares_ratio DECIMAL(8,4) DEFAULT 0,
    foreign_investment_remain_ratio DECIMAL(8,4) DEFAULT 0,
    foreign_investment_upper_limit_ratio DECIMAL(8,4) DEFAULT 0,
    number_of_shares_issued BIGINT DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_foreign_stock_date UNIQUE (stock_symbol, trade_date)
);

CREATE INDEX idx_foreign_trade_date ON foreign_shareholding_stats(trade_date);
CREATE INDEX idx_foreign_stock_symbol ON foreign_shareholding_stats(stock_symbol);

-- 集保分散表
CREATE TABLE shareholding_distribution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_symbol VARCHAR(20) NOT NULL,
    data_date DATE NOT NULL,
    holding_shares_level VARCHAR(50) NOT NULL,

    people BIGINT DEFAULT 0,
    percent DECIMAL(8,4) DEFAULT 0,
    unit BIGINT DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_distribution_stock_date_level UNIQUE (stock_symbol, data_date, holding_shares_level)
);

CREATE INDEX idx_distribution_data_date ON shareholding_distribution(data_date);
CREATE INDEX idx_distribution_stock_symbol ON shareholding_distribution(stock_symbol);
