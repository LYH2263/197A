-- 兼容旧库：若 user 表无 role 列则添加（新库 01 已含 role 则跳过）
SET NAMES utf8mb4;
USE shop;

DROP PROCEDURE IF EXISTS add_user_role_column;
DELIMITER //
CREATE PROCEDURE add_user_role_column()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'role') = 0 THEN
    ALTER TABLE `user` ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色 admin=管理员 user=普通用户' AFTER `status`;
    UPDATE `user` SET `role` = 'admin' WHERE username = 'admin';
  END IF;
END //
DELIMITER ;
CALL add_user_role_column();
DROP PROCEDURE add_user_role_column;
