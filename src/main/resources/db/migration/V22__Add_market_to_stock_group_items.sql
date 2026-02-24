-- V22: 新增 market 欄位至 stock_group_items 資料表
--
-- 目的：記錄股票所屬市場（如：TSE 上市、OTC 上櫃），支援跨市場股票查詢

ALTER TABLE stock_group_items ADD COLUMN market VARCHAR(10) COMMENT '股票所屬市場（如：TSE, OTC）';
