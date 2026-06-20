SET NAMES utf8mb4;
USE shop;

CREATE TABLE IF NOT EXISTS `product_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `image_url` VARCHAR(512) NOT NULL COMMENT '图片URL',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号，越小越靠前',
  `is_main` TINYINT NOT NULL DEFAULT 0 COMMENT '是否为主图 0否 1是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_image` (`product_id`, `image_url`),
  KEY `idx_product` (`product_id`),
  KEY `idx_product_sort` (`product_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品图片关联表';

INSERT IGNORE INTO `product_image` (`product_id`, `image_url`, `sort_order`, `is_main`) VALUES
(1, '/images/phone-pro.png', 0, 1),
(1, '/images/phone1.svg', 1, 0),
(1, '/images/phone2.svg', 2, 0),
(1, '/images/phone-lite.png', 3, 0),
(2, '/images/phone-lite.png', 0, 1),
(2, '/images/phone2.svg', 1, 0),
(2, '/images/phone1.svg', 2, 0),
(3, '/images/laptop-thin.png', 0, 1),
(3, '/images/laptop1.svg', 1, 0),
(3, '/images/laptop2.svg', 2, 0),
(3, '/images/laptop-gaming.png', 3, 0),
(4, '/images/laptop-gaming.png', 0, 1),
(4, '/images/laptop2.svg', 1, 0),
(4, '/images/laptop1.svg', 2, 0),
(5, '/images/shirt-mens.png', 0, 1),
(5, '/images/shirt1.svg', 1, 0),
(5, '/images/dress1.svg', 2, 0),
(6, '/images/dress-womens.png', 0, 1),
(6, '/images/dress1.svg', 1, 0),
(6, '/images/shirt1.svg', 2, 0);
