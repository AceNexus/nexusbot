-- V23: 建立三大法人統計資料表
--
-- 目的：儲存每日三大法人（外資、投信、自營商）買賣超統計資料，支援籌碼分析功能

CREATE TABLE institutional_investor_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主鍵，自動遞增',
    stock_symbol VARCHAR(20) NOT NULL COMMENT '股票代號',
    stock_name VARCHAR(100) COMMENT '股票名稱',
    trade_date DATE NOT NULL COMMENT '交易日期',

    foreign_investor_buy BIGINT DEFAULT 0 COMMENT '外資買進張數',
    foreign_investor_sell BIGINT DEFAULT 0 COMMENT '外資賣出張數',
    foreign_investor_buy_sell BIGINT DEFAULT 0 COMMENT '外資買賣超張數',

    investment_trust_buy BIGINT DEFAULT 0 COMMENT '投信買進張數',
    investment_trust_sell BIGINT DEFAULT 0 COMMENT '投信賣出張數',
    investment_trust_buy_sell BIGINT DEFAULT 0 COMMENT '投信買賣超張數',

    dealer_buy BIGINT DEFAULT 0 COMMENT '自營商買進張數',
    dealer_sell BIGINT DEFAULT 0 COMMENT '自營商賣出張數',
    dealer_buy_sell BIGINT DEFAULT 0 COMMENT '自營商買賣超張數',

    total_buy BIGINT DEFAULT 0 COMMENT '三大法人合計買進張數',
    total_sell BIGINT DEFAULT 0 COMMENT '三大法人合計賣出張數',
    total_buy_sell BIGINT DEFAULT 0 COMMENT '三大法人合計買賣超張數',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',

    CONSTRAINT uk_stock_date UNIQUE (stock_symbol, trade_date)
);

CREATE INDEX idx_trade_date ON institutional_investor_stats(trade_date);
CREATE INDEX idx_stock_symbol ON institutional_investor_stats(stock_symbol);
