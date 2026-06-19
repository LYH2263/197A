-- 演示数据 Seed（utf8mb4）
SET NAMES utf8mb4;
USE shop;

-- 用户（密码均为 123456，BCrypt 加密后的值；admin 为管理员角色）
INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `email`, `status`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', '13800000001', 'admin@shop.com', 1, 'admin'),
('user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', '13800000002', 'user1@shop.com', 1, 'user'),
('user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', '13800000003', 'user2@shop.com', 1, 'user');

-- 分类
INSERT INTO `category` (`name`, `parent_id`, `sort_order`) VALUES
('数码电子', 0, 1),
('服装鞋帽', 0, 2),
('家居生活', 0, 3),
('手机', 1, 1),
('笔记本', 1, 2),
('男装', 2, 1),
('女装', 2, 2);

-- 商品（main_image 使用本地拟真商品图）
INSERT INTO `product` (`category_id`, `name`, `subtitle`, `main_image`, `price`, `stock`, `status`) VALUES
(4, '智能手机 Pro', '旗舰芯片 超清影像', '/images/phone-pro.png', 3999.00, 100, 1),
(4, '智能手机 Lite', '高性价比 长续航', '/images/phone-lite.png', 1999.00, 200, 1),
(5, '轻薄笔记本', '14英寸 办公学习', '/images/laptop-thin.png', 4999.00, 50, 1),
(5, '游戏笔记本', '独显 高刷屏', '/images/laptop-gaming.png', 7999.00, 30, 1),
(6, '男士休闲衬衫', '纯棉 多色', '/images/shirt-mens.png', 199.00, 300, 1),
(7, '女士连衣裙', '夏季新款', '/images/dress-womens.png', 299.00, 150, 1);
