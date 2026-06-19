-- 购物车增加价格快照字段，用于记录加购时价格
ALTER TABLE cart_item ADD COLUMN IF NOT EXISTS price_snapshot DECIMAL(12,2) DEFAULT NULL COMMENT '加购时价格快照';
