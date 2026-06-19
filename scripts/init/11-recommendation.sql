USE shop;

CREATE TABLE IF NOT EXISTS `site_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `config_key` VARCHAR(128) NOT NULL COMMENT '配置键',
  `config_value` TEXT NOT NULL COMMENT '配置值(JSON)',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站点配置';

CREATE TABLE IF NOT EXISTS `browse_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `category_id` BIGINT NOT NULL COMMENT '商品分类ID(快照)',
  `viewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  KEY `idx_user_category` (`user_id`, `category_id`, `viewed_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='浏览历史';

ALTER TABLE `product` ADD INDEX `idx_sales_category` (`category_id`, `sales_count` DESC);

INSERT INTO `site_config` (`config_key`, `config_value`) VALUES
('recommendation', '{"guessYouLikeEnabled":true,"guessYouLikeCount":6,"viewedAlsoViewEnabled":true,"viewedAlsoViewCount":6}')
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);
