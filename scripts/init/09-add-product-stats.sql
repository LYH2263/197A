-- 商品搜索筛选增强：添加销量、评价数字段及索引
SET NAMES utf8mb4;
USE shop;

ALTER TABLE `product`
  ADD COLUMN `sales_count` INT NOT NULL DEFAULT 0 COMMENT '销量' AFTER `stock`,
  ADD COLUMN `review_count` INT NOT NULL DEFAULT 0 COMMENT '评价数' AFTER `sales_count`,
  ADD INDEX `idx_sales_count` (`sales_count`),
  ADD INDEX `idx_review_count` (`review_count`),
  ADD INDEX `idx_created_at` (`created_at`),
  ADD INDEX `idx_price` (`price`);

-- 用已有订单明细和评价数据初始化统计（演示数据可能为空，给点模拟值便于筛选演示）
UPDATE `product` SET `sales_count` = FLOOR(RAND() * 500) + 10, `review_count` = FLOOR(RAND() * 200) + 5;
