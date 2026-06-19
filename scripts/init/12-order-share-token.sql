USE shop;

CREATE TABLE IF NOT EXISTS `order_share_token` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `order_id`   BIGINT       NOT NULL COMMENT '订单ID',
    `token`      VARCHAR(128) NOT NULL COMMENT '分享令牌',
    `expires_at` DATETIME     NOT NULL COMMENT '过期时间',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单分享令牌表';
