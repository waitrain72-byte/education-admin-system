package com.example.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时的增量表结构升级（幂等）：新版本代码新增的列，在已有数据库上自动补齐。
 *
 * <p>项目用整库备份 sql/xm_educational_manager-full.sql 初始化数据库；已经在用的库重新导入会丢数据，
 * 因此在启动时查 information_schema，缺哪列补哪列。新导入最新备份的库已经含有这些列，这里什么也不做。</p>
 *
 * <p>补列失败（例如数据库账号没有 ALTER 权限）只记错误日志并给出可手动执行的 SQL，不阻止后端启动。</p>
 */
@Component
public class SchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 考试安排：考试时间（Web / 小程序据此显示考试倒计时）
        addColumnIfMissing("examplan", "exam_time",
                "varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考试时间（yyyy-MM-dd HH:mm）' AFTER `time`");
    }

    /**
     * 列不存在时执行 ALTER TABLE ... ADD COLUMN。表名、列名、列定义都是代码里的常量，不接受外部输入。
     *
     * @return 本次是否新增了列
     */
    boolean addColumnIfMissing(String table, String column, String definition) {
        String ddl = "ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition;
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS"
                            + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, table, column);
            if (count != null && count > 0) {
                return false;
            }
            jdbcTemplate.execute(ddl);
            log.info("数据库升级：{} 表已新增列 {}", table, column);
            return true;
        } catch (Exception e) {
            log.error("数据库升级失败：{} 表缺少列 {}，请手动执行：{};", table, column, ddl, e);
            return false;
        }
    }
}
