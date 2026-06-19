-- 订单主表增加地址关联字段（如果不存在）
ALTER TABLE `order_main` ADD COLUMN IF NOT EXISTS `shipping_address_id` BIGINT DEFAULT NULL COMMENT '关联地址ID' AFTER `receiver_address`;
ALTER TABLE `order_main` ADD KEY IF NOT EXISTS `idx_shipping_address` (`shipping_address_id`);

-- 收货地址表
CREATE TABLE IF NOT EXISTS `shipping_address` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `receiver_name` VARCHAR(64) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人手机号',
  `province` VARCHAR(32) NOT NULL COMMENT '省份',
  `city` VARCHAR(32) NOT NULL COMMENT '城市',
  `district` VARCHAR(32) NOT NULL COMMENT '区/县',
  `detail_address` VARCHAR(256) NOT NULL COMMENT '详细地址',
  `tag` VARCHAR(20) DEFAULT NULL COMMENT '标签：家/公司/其他',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址：0否 1是',
  `is_disabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否禁用(不可修改删除)：0否 1是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_user_default` (`user_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收货地址表';
