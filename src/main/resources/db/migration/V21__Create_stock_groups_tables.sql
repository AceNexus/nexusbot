-- V21: 建立股票群組相關資料表
--
-- 目的：支援使用者自訂股票群組功能，允許分組管理追蹤中的股票
--
-- 包含兩張表：
-- 1. stock_groups      — 股票群組定義表
-- 2. stock_group_items — 群組內股票明細表

-- 1. 股票群組定義表
CREATE TABLE stock_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '群組 ID，自動遞增',
    user_id VARCHAR(100) NOT NULL COMMENT 'LINE userId 或匿名 UUID',
    name VARCHAR(100) NOT NULL COMMENT '群組名稱',
    display_order INT NOT NULL DEFAULT 0 COMMENT '顯示排序',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否啟用，FALSE 表示已軟刪除',
    is_selected BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否為目前選取的群組（用於狀態還原）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間'
);

-- 2. 群組內股票明細表
CREATE TABLE stock_group_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '明細 ID，自動遞增',
    group_id BIGINT NOT NULL COMMENT '所屬群組 ID，對應 stock_groups.id',
    stock_symbol VARCHAR(20) NOT NULL COMMENT '股票代號（如：2330）',
    stock_name VARCHAR(100) COMMENT '股票名稱（如：台積電）',
    display_order INT NOT NULL DEFAULT 0 COMMENT '群組內顯示排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間'
);

-- 索引
CREATE INDEX idx_stock_groups_user_id ON stock_groups(user_id);
CREATE INDEX idx_stock_groups_user_active ON stock_groups(user_id, is_active);
CREATE INDEX idx_stock_group_items_group ON stock_group_items(group_id);
CREATE UNIQUE INDEX idx_stock_group_items_unique ON stock_group_items(group_id, stock_symbol);
