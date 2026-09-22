-- ============================================================
-- 迁移脚本：为三张账号表增加「自定义主题色」列
--
-- 适用场景：数据库已经用旧版 xm_educational_manager-full.sql 导入过，
--          不想重导库、要保留现有数据时执行本脚本即可。
--          全新导入的环境无需执行 —— 种子文件里已经包含该列。
--
-- 用法：
--   mysql -uroot -p123456 xm_educational_manager < sql/migration-theme-color.sql
--
-- 说明：
--   - 只做 ADD COLUMN，不动任何现有数据，可安全重复执行前先看下面的幂等提示；
--   - MySQL 5.7 不支持 ADD COLUMN IF NOT EXISTS，重复执行会报
--     "Duplicate column name 'theme_color'"，这属于正常报错，忽略即可；
--   - 空串表示「用系统内置默认色」，所以存量账号迁移后外观完全不变。
-- ============================================================

ALTER TABLE `admin`
    ADD COLUMN `theme_color` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT ''
        COMMENT '自定义主题色: #RRGGBB，空串表示用内置默认色' AFTER `locale`;

ALTER TABLE `teacher`
    ADD COLUMN `theme_color` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT ''
        COMMENT '自定义主题色: #RRGGBB，空串表示用内置默认色' AFTER `locale`;

ALTER TABLE `student`
    ADD COLUMN `theme_color` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT ''
        COMMENT '自定义主题色: #RRGGBB，空串表示用内置默认色' AFTER `locale`;
