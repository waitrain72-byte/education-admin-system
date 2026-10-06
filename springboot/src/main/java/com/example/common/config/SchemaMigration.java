package com.example.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 启动时的增量表结构升级（幂等）：新版本代码新增的表、列、权限点和系统参数，在已有数据库上自动补齐。
 *
 * <p>项目用整库备份 sql/xm_educational_manager-full.sql 初始化数据库；已经在用的库重新导入会丢数据，
 * 因此在启动时查 information_schema，缺什么补什么。新导入最新备份的库已经都有，这里什么也不做。</p>
 *
 * <p>每一步各自捕获异常：某一步失败（例如数据库账号没有 ALTER / CREATE 权限）只记错误日志并给出可手动执行的 SQL，
 * 不影响其余步骤，也不阻止后端启动。</p>
 */
@Component
public class SchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigration.class);

    /** 与种子里的业务表保持一致的表选项 */
    private static final String TABLE_OPTIONS =
            " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC";

    static final String DDL_SYS_CONFIG = "CREATE TABLE `sys_config` ("
            + "`config_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参数键',"
            + "`config_value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '参数值',"
            + "`remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '说明',"
            + "PRIMARY KEY (`config_key`)"
            + ")" + TABLE_OPTIONS + " COMMENT='系统参数（学期名称、开学日期、教学周数）'";

    static final String DDL_MESSAGE = "CREATE TABLE `message` ("
            + "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',"
            + "`user_id` int(11) NOT NULL COMMENT '接收人ID',"
            + "`role` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接收人角色',"
            + "`type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型：score/homework/apply/warning/attendance/course',"
            + "`title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',"
            + "`content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内容',"
            + "`link` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '点击后跳转的前端路径',"
            + "`is_read` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已读',"
            + "`create_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '时间（yyyy-MM-dd HH:mm）',"
            + "PRIMARY KEY (`id`),"
            + "KEY `idx_message_receiver` (`user_id`,`role`,`is_read`)"
            + ")" + TABLE_OPTIONS + " COMMENT='站内消息'";

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public SchemaMigration(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Clock.systemDefaultZone());
    }

    @Autowired
    public SchemaMigration(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 考试安排：考试时间（Web / 小程序据此显示考试倒计时）
        addColumnIfMissing("examplan", "exam_time",
                "varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考试时间（yyyy-MM-dd HH:mm）' AFTER `time`");

        // 系统参数：学期名称、开学日期、教学周数（首页「第几周」、日程按它推算）
        if (createTableIfMissing("sys_config", DDL_SYS_CONFIG)) {
            String[] semester = defaultSemester(LocalDate.now(clock));
            ensureConfig("semester_name", semester[0], "当前学期名称");
            ensureConfig("semester_start", semester[1], "开学日期（第 1 教学周的周一，yyyy-MM-dd）");
            ensureConfig("semester_weeks", "18", "教学周数");
        }

        // 站内消息：成绩发布、作业批改、请假审核等推送落库，消息中心可回看
        createTableIfMissing("message", DDL_MESSAGE);

        // 权限点：学期设置（仅管理员）
        ensurePermission("config:manage", "学期设置", "menu", "config", 1, "ADMIN");
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

    /**
     * 表不存在时执行建表语句。DDL 是代码里的常量，不接受外部输入。
     *
     * @return 本次是否新建了表
     */
    boolean createTableIfMissing(String table, String ddl) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                    Integer.class, table);
            if (count != null && count > 0) {
                return false;
            }
            jdbcTemplate.execute(ddl);
            log.info("数据库升级：已新建表 {}", table);
            return true;
        } catch (Exception e) {
            log.error("数据库升级失败：缺少表 {}，请手动执行：{};", table, ddl, e);
            return false;
        }
    }

    /**
     * 权限点不存在时新增，并授予给定角色。
     *
     * <p>只在「本次新插入」时授权：权限点已存在说明管理员可能已在权限设置页调整过授权，
     * 这时不能再按默认值改动，否则每次重启都会把管理员撤掉的授权加回来。</p>
     *
     * @return 本次是否新增了权限点
     */
    boolean ensurePermission(String code, String name, String type, String module, int sortNum, String... roleCodes) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_permission WHERE code = ?", Integer.class, code);
            if (count != null && count > 0) {
                return false;
            }
            jdbcTemplate.update("INSERT INTO sys_permission (code, name, type, module, sort_num) VALUES (?, ?, ?, ?, ?)",
                    code, name, type, module, sortNum);
            for (String roleCode : roleCodes) {
                jdbcTemplate.update("INSERT INTO sys_role_permission (role_id, permission_id)"
                        + " SELECT r.id, p.id FROM sys_role r JOIN sys_permission p ON p.code = ? WHERE r.code = ?",
                        code, roleCode);
            }
            log.info("数据库升级：已新增权限点 {}", code);
            return true;
        } catch (Exception e) {
            log.error("数据库升级失败：权限点 {} 未能写入，可在权限设置页确认后手动补充", code, e);
            return false;
        }
    }

    /** 系统参数不存在时写入默认值（已存在则保留管理员改过的值） */
    void ensureConfig(String key, String value, String remark) {
        try {
            jdbcTemplate.update("INSERT IGNORE INTO sys_config (config_key, config_value, remark) VALUES (?, ?, ?)",
                    key, value, remark);
        } catch (Exception e) {
            log.error("数据库升级失败：系统参数 {} 未能写入", key, e);
        }
    }

    /**
     * 首次建表时按当前日期推一个看起来合理的默认学期：8 月到次年 1 月算第一学期（9 月 1 日所在周开学），
     * 2 月到 7 月算第二学期（3 月 1 日所在周开学）。管理员随后可在「学期设置」里改成学校的真实校历。
     *
     * @return [学期名称, 开学日期 yyyy-MM-dd]
     */
    static String[] defaultSemester(LocalDate today) {
        int year = today.getYear();
        int month = today.getMonthValue();
        if (month >= 8 || month == 1) {
            int startYear = month == 1 ? year - 1 : year;
            LocalDate start = LocalDate.of(startYear, 9, 1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return new String[]{startYear + "-" + (startYear + 1) + " 学年第一学期", start.toString()};
        }
        LocalDate start = LocalDate.of(year, 3, 1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new String[]{(year - 1) + "-" + year + " 学年第二学期", start.toString()};
    }
}
