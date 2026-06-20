-- 购物车增加价格快照字段，用于记录加购时价格
SET NAMES utf8mb4;
USE shop;

DROP PROCEDURE IF EXISTS add_cart_price_snapshot;
DELIMITER //
CREATE PROCEDURE add_cart_price_snapshot()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cart_item' AND COLUMN_NAME = 'price_snapshot') = 0 THEN
    ALTER TABLE `cart_item`
      ADD COLUMN `price_snapshot` DECIMAL(12,2) DEFAULT NULL COMMENT '加购时价格快照';
  END IF;
END //
DELIMITER ;
CALL add_cart_price_snapshot();
DROP PROCEDURE add_cart_price_snapshot;
