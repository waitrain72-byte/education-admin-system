-- 界面语言偏好字段迁移脚本：为三类账号表增加 locale 字段
-- 取值：'zh-CN' | 'en-US'，默认中文
-- 全新环境无需手动执行：docker-compose 会按 01-init → 02-theme → 03-locale 顺序自动导入
-- 已有环境执行：mysql -uroot -p xm_educational_manager < add_locale_column.sql

ALTER TABLE admin   ADD COLUMN locale VARCHAR(10) NOT NULL DEFAULT 'zh-CN' COMMENT '界面语言: zh-CN/en-US';
ALTER TABLE teacher ADD COLUMN locale VARCHAR(10) NOT NULL DEFAULT 'zh-CN' COMMENT '界面语言: zh-CN/en-US';
ALTER TABLE student ADD COLUMN locale VARCHAR(10) NOT NULL DEFAULT 'zh-CN' COMMENT '界面语言: zh-CN/en-US';
