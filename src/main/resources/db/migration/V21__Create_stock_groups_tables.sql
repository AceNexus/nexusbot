-- Stock Groups Table
-- 用於儲存使用者自訂的股票群組
CREATE TABLE stock_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,              -- LINE userId or anonymous UUID
    name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,    -- Soft delete
    is_selected BOOLEAN NOT NULL DEFAULT FALSE, -- State restoration
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Stock Group Items Table
-- 用於儲存群組內的股票
CREATE TABLE stock_group_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    stock_symbol VARCHAR(20) NOT NULL,
    stock_name VARCHAR(100),
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_stock_groups_user_id ON stock_groups(user_id);
CREATE INDEX idx_stock_groups_user_active ON stock_groups(user_id, is_active);
CREATE INDEX idx_stock_group_items_group ON stock_group_items(group_id);
CREATE UNIQUE INDEX idx_stock_group_items_unique ON stock_group_items(group_id, stock_symbol);
