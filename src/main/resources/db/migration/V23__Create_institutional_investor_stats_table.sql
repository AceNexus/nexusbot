CREATE TABLE institutional_investor_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_symbol VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100),
    trade_date DATE NOT NULL,
    
    foreign_investor_buy BIGINT DEFAULT 0,
    foreign_investor_sell BIGINT DEFAULT 0,
    foreign_investor_buy_sell BIGINT DEFAULT 0,
    
    investment_trust_buy BIGINT DEFAULT 0,
    investment_trust_sell BIGINT DEFAULT 0,
    investment_trust_buy_sell BIGINT DEFAULT 0,
    
    dealer_buy BIGINT DEFAULT 0,
    dealer_sell BIGINT DEFAULT 0,
    dealer_buy_sell BIGINT DEFAULT 0,
    
    total_buy BIGINT DEFAULT 0,
    total_sell BIGINT DEFAULT 0,
    total_buy_sell BIGINT DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_stock_date UNIQUE (stock_symbol, trade_date)
);

CREATE INDEX idx_trade_date ON institutional_investor_stats(trade_date);
CREATE INDEX idx_stock_symbol ON institutional_investor_stats(stock_symbol);
