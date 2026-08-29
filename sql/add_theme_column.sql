-- 主题偏好字段迁移脚本：为三类账号表增加 theme 字段
-- 取值：'light' | 'dark' | 'system'，默认跟随系统
-- 执行方式：mysql -uroot -p xm_educational_manager < add_theme_column.sql
--          或在 Navicat / MySQL 命令行中直接执行以下语句

ALTER TABLE admin   ADD COLUMN theme VARCHAR(10) NOT NULL DEFAULT 'system' COMMENT '主题偏好: light/dark/system';
ALTER TABLE teacher ADD COLUMN theme VARCHAR(10) NOT NULL DEFAULT 'system' COMMENT '主题偏好: light/dark/system';
ALTER TABLE student ADD COLUMN theme VARCHAR(10) NOT NULL DEFAULT 'system' COMMENT '主题偏好: light/dark/system';
