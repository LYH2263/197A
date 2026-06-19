-- 订单状态机迭代扩展脚本
-- 1. 扩展 order_main 表增加物流与发货相关字段
-- 2. 创建订单操作日志表 order_operation_log
-- 3. 创建售后意图表 after_sale_intent

USE shop;

-- ============================================================
-- 1. 扩展 order_main 表
-- ============================================================
ALTER TABLE `order_main`
    ADD COLUMN `logistics_company` VARCHAR(64)  DEFAULT NULL COMMENT '物流公司名称' AFTER `receiver_address`,
    ADD COLUMN `tracking_no`       VARCHAR(64)  DEFAULT NULL COMMENT '运单号'         AFTER `logistics_company`,
    ADD COLUMN `shipping_remark`   VARCHAR(256) DEFAULT NULL COMMENT '发货备注'       AFTER `tracking_no`,
    ADD COLUMN `shipped_at`        DATETIME     DEFAULT NULL COMMENT '发货时间'       AFTER `shipping_remark`,
    ADD COLUMN `completed_at`      DATETIME     DEFAULT NULL COMMENT '确认收货时间'   AFTER `shipped_at`;

-- ============================================================
-- 2. 订单操作日志表（记录操作人、时间、原状态与新状态）
-- ============================================================
CREATE TABLE IF NOT EXISTS `order_operation_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `order_id`      BIGINT       NOT NULL COMMENT '订单ID',
    `order_no`      VARCHAR(32)  NOT NULL COMMENT '订单号（冗余便于查询）',
    `operator_id`   BIGINT       DEFAULT NULL COMMENT '操作人ID（NULL表示系统自动操作）',
    `operator_name` VARCHAR(64)  DEFAULT NULL COMMENT '操作人姓名/用户名（冗余）',
    `operator_role` VARCHAR(20)  DEFAULT NULL COMMENT '操作人角色 admin/user/system',
    `operation`     VARCHAR(32)  NOT NULL COMMENT '操作类型 CREATE/PAY/CANCEL/SHIP/RECEIVE/AFTER_SALE_INTENT',
    `old_status`    TINYINT      DEFAULT NULL COMMENT '原状态',
    `new_status`    TINYINT      DEFAULT NULL COMMENT '新状态',
    `remark`        VARCHAR(512) DEFAULT NULL COMMENT '操作备注（如发货备注）',
    `extra_info`    TEXT         DEFAULT NULL COMMENT '扩展信息 JSON（如物流公司、运单号）',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_id`  (`order_id`),
    KEY `idx_order_no`  (`order_no`),
    KEY `idx_operator`  (`operator_id`),
    KEY `idx_operation` (`operation`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单操作日志表';

-- ============================================================
-- 3. 售后意图表（记录用户点击申请售后的 intent）
-- ============================================================
CREATE TABLE IF NOT EXISTS `after_sale_intent` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `order_id`      BIGINT       NOT NULL COMMENT '订单ID',
    `order_no`      VARCHAR(32)  NOT NULL COMMENT '订单号（冗余）',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `intent_source` VARCHAR(32)  DEFAULT 'DETAIL' COMMENT '来源 LIST/DETAIL',
    `triggered_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
    `handled`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已处理 0否 1是',
    `handled_at`    DATETIME     DEFAULT NULL COMMENT '处理时间',
    `remark`        VARCHAR(512) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id`  (`user_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_triggered` (`triggered_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='售后意图记录表';
