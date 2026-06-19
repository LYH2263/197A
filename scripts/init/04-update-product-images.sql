-- 将商品主图更新为本地拟真图（适用于已初始化过的数据库）
SET NAMES utf8mb4;
USE shop;

UPDATE `product` SET `main_image` = '/images/phone-pro.png'    WHERE `name` = '智能手机 Pro';
UPDATE `product` SET `main_image` = '/images/phone-lite.png'   WHERE `name` = '智能手机 Lite';
UPDATE `product` SET `main_image` = '/images/laptop-thin.png'  WHERE `name` = '轻薄笔记本';
UPDATE `product` SET `main_image` = '/images/laptop-gaming.png' WHERE `name` = '游戏笔记本';
UPDATE `product` SET `main_image` = '/images/shirt-mens.png'    WHERE `name` = '男士休闲衬衫';
UPDATE `product` SET `main_image` = '/images/dress-womens.png'  WHERE `name` = '女士连衣裙';
