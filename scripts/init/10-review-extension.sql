-- 评价模块迭代：扩展 review 表支持追评、图片、官方回复
-- 执行时间: 2026-06-20

USE shop;

-- 扩展 review 表
ALTER TABLE `review`
    ADD COLUMN `parent_id` BIGINT NULL DEFAULT NULL COMMENT '父评价ID（追评关联初评）' AFTER `id`,
    ADD COLUMN `review_type` TINYINT NOT NULL DEFAULT 0 COMMENT '评价类型：0=初评，1=追评' AFTER `parent_id`,
    ADD COLUMN `images` VARCHAR(1024) DEFAULT NULL COMMENT '评价图片URL列表（JSON数组）' AFTER `content`,
    ADD COLUMN `reply_content` VARCHAR(512) DEFAULT NULL COMMENT '商家官方回复内容' AFTER `images`,
    ADD COLUMN `reply_at` DATETIME DEFAULT NULL COMMENT '商家官方回复时间' AFTER `reply_content`;

-- 添加索引
ALTER TABLE `review` ADD KEY `idx_parent_id` (`parent_id`);
ALTER TABLE `review` ADD KEY `idx_review_type` (`review_type`);
