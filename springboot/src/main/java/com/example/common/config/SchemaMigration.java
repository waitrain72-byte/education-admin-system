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

    static final String DDL_ASSIGNMENT = "CREATE TABLE `assignment` ("
            + "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',"
            + "`course_id` int(11) NOT NULL COMMENT '课程ID',"
            + "`teacher_id` int(11) DEFAULT NULL COMMENT '布置的教师',"
            + "`title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作业标题',"
            + "`content` text COLLATE utf8mb4_unicode_ci COMMENT '作业要求',"
            + "`attachment` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件地址',"
            + "`attachment_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件原文件名',"
            + "`deadline` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '截止时间（yyyy-MM-dd HH:mm）',"
            + "`full_score` int(11) NOT NULL DEFAULT '100' COMMENT '满分',"
            + "`create_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '布置时间（yyyy-MM-dd HH:mm）',"
            + "PRIMARY KEY (`id`),"
            + "KEY `idx_assignment_course` (`course_id`)"
            + ")" + TABLE_OPTIONS + " COMMENT='作业任务（老师布置，学生按任务提交）'";

    static final String DDL_ATTENDANCE_SESSION = "CREATE TABLE `attendance_session` ("
            + "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',"
            + "`course_id` int(11) NOT NULL COMMENT '课程ID',"
            + "`teacher_id` int(11) DEFAULT NULL COMMENT '发起的教师',"
            + "`code` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '签到码',"
            + "`date` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '上课日期（yyyy-MM-dd，写入考勤记录的时间）',"
            + "`start_time` varchar(19) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发起时间（yyyy-MM-dd HH:mm:ss）',"
            + "`expire_time` varchar(19) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '截止时间（yyyy-MM-dd HH:mm:ss）',"
            + "`status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '进行中' COMMENT '进行中 / 已结束',"
            + "PRIMARY KEY (`id`),"
            + "KEY `idx_session_course` (`course_id`, `status`)"
            + ")" + TABLE_OPTIONS + " COMMENT='课堂签到场次'";

    static final String DDL_COURSE_EVAL = "CREATE TABLE `course_eval` ("
            + "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',"
            + "`course_id` int(11) NOT NULL COMMENT '课程ID',"
            + "`teacher_id` int(11) DEFAULT NULL COMMENT '任课教师',"
            + "`student_id` int(11) NOT NULL COMMENT '评价的学生（对教师匿名展示）',"
            + "`attitude` tinyint(4) NOT NULL COMMENT '教学态度 1-5',"
            + "`content_score` tinyint(4) NOT NULL COMMENT '教学内容 1-5',"
            + "`method` tinyint(4) NOT NULL COMMENT '教学方法 1-5',"
            + "`effect` tinyint(4) NOT NULL COMMENT '教学效果 1-5',"
            + "`support` tinyint(4) NOT NULL COMMENT '课后辅导 1-5',"
            + "`comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文字评价',"
            + "`create_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评价时间（yyyy-MM-dd HH:mm）',"
            + "PRIMARY KEY (`id`),"
            + "UNIQUE KEY `uk_eval_course_student` (`course_id`, `student_id`),"
            + "KEY `idx_eval_teacher` (`teacher_id`)"
            + ")" + TABLE_OPTIONS + " COMMENT='课程评价（五个维度打分 + 文字）'";

    static final String DDL_COURSE_RESOURCE = "CREATE TABLE `course_resource` ("
            + "`id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',"
            + "`course_id` int(11) NOT NULL COMMENT '课程ID',"
            + "`name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资料名称（原文件名）',"
            + "`file` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件地址',"
            + "`size` bigint(20) DEFAULT NULL COMMENT '字节数',"
            + "`uploader_id` int(11) DEFAULT NULL COMMENT '上传人ID',"
            + "`uploader_role` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上传人角色',"
            + "`create_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上传时间（yyyy-MM-dd HH:mm）',"
            + "PRIMARY KEY (`id`),"
            + "KEY `idx_resource_course` (`course_id`)"
            + ")" + TABLE_OPTIONS + " COMMENT='课程资料'";

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

        migrateCourseSpace();
    }

    /** 课程空间：作业任务、课堂签到、课程评价、课程资料、成绩册权重与发布、课程公告 */
    private void migrateCourseSpace() {
        createTableIfMissing("assignment", DDL_ASSIGNMENT);
        createTableIfMissing("attendance_session", DDL_ATTENDANCE_SESSION);
        createTableIfMissing("course_eval", DDL_COURSE_EVAL);
        createTableIfMissing("course_resource", DDL_COURSE_RESOURCE);

        // 作业提交挂到作业任务下；旧版「学生直接上传」的记录 assignment_id 为空，照常可查
        addColumnIfMissing("homework", "assignment_id",
                "int(11) DEFAULT NULL COMMENT '所属作业任务（旧版自由提交为空）' AFTER `id`");
        addColumnIfMissing("homework", "submit_time",
                "varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提交时间（yyyy-MM-dd HH:mm）'");
        addColumnIfMissing("homework", "status",
                "varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '已提交 / 已批改'");
        addColumnIfMissing("homework", "file_name",
                "varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件原文件名'");
        addIndexIfMissing("homework", "idx_homework_assignment", "`assignment_id`");

        // 考勤记录关联签到场次（老师手工登记的为空）
        addColumnIfMissing("attendance", "session_id",
                "int(11) DEFAULT NULL COMMENT '签到场次（老师手工登记为空）'");

        // 成绩册：考勤分、作业分两项成绩，以及草稿 / 已发布（老数据都算已发布）
        addColumnIfMissing("score", "attendance_score",
                "double(10,2) DEFAULT NULL COMMENT '考勤分' AFTER `teacher_id`");
        addColumnIfMissing("score", "homework_score",
                "double(10,2) DEFAULT NULL COMMENT '作业分' AFTER `attendance_score`");
        addColumnIfMissing("score", "status",
                "varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '已发布' COMMENT '草稿 / 已发布（学生只看得到已发布）'");

        // 课程的总评权重（%）：默认 平时 30 + 期末 70，与改版前的计算方式一致；以及课程简介
        addColumnIfMissing("course", "weight_attendance", "int(11) NOT NULL DEFAULT '0' COMMENT '总评权重：考勤（%）'");
        addColumnIfMissing("course", "weight_homework", "int(11) NOT NULL DEFAULT '0' COMMENT '总评权重：作业（%）'");
        addColumnIfMissing("course", "weight_ordinary", "int(11) NOT NULL DEFAULT '30' COMMENT '总评权重：平时（%）'");
        addColumnIfMissing("course", "weight_exam", "int(11) NOT NULL DEFAULT '70' COMMENT '总评权重：期末（%）'");
        addColumnIfMissing("course", "intro", "varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '课程简介'");

        // 课程公告：与全校通知同表，course_id 非空即课程公告
        addColumnIfMissing("notice", "course_id",
                "int(11) DEFAULT NULL COMMENT '所属课程（空 = 全校通知，非空 = 课程公告）'");
        addIndexIfMissing("notice", "idx_notice_course", "`course_id`");

        ensurePermission("assignment:view", "作业任务-查看", "menu", "assignment", 1, "ADMIN", "TEACHER", "STUDENT");
        ensurePermission("assignment:manage", "作业任务-布置/批改", "button", "assignment", 2, "ADMIN", "TEACHER");
        ensurePermission("assignment:submit", "作业任务-提交作业", "button", "assignment", 3, "STUDENT");
        ensurePermission("course:teach", "课程空间-发公告/改简介", "button", "course", 3, "ADMIN", "TEACHER");
        ensurePermission("attendance:checkin", "考勤-课堂签到", "button", "attendance", 3, "STUDENT");
        ensurePermission("resource:view", "课程资料-查看", "menu", "resource", 1, "ADMIN", "TEACHER", "STUDENT");
        ensurePermission("resource:manage", "课程资料-上传/删除", "button", "resource", 2, "ADMIN", "TEACHER");
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
     * 索引不存在时补建。表名、索引名、列都是代码里的常量，不接受外部输入。
     *
     * @return 本次是否新建了索引
     */
    boolean addIndexIfMissing(String table, String index, String columns) {
        String ddl = "ALTER TABLE `" + table + "` ADD INDEX `" + index + "` (" + columns + ")";
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.STATISTICS"
                            + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                    Integer.class, table, index);
            if (count != null && count > 0) {
                return false;
            }
            jdbcTemplate.execute(ddl);
            log.info("数据库升级：{} 表已新增索引 {}", table, index);
            return true;
        } catch (Exception e) {
            log.error("数据库升级失败：{} 表缺少索引 {}，可手动执行：{};", table, index, ddl, e);
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
