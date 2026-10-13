package com.example.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 管理员首页指标与数据大屏的聚合统计（只读查询，全部 #{} 预编译）。
 *
 * <p>带 {@code teacherId} 参数的查询：传 null 统计全校（管理员），传教师 id 只统计该教师任课的数据。
 * 时间参数与库里的字符串时间同格式（日期 yyyy-MM-dd，时间 yyyy-MM-dd HH:mm），直接按字符串比较。</p>
 */
public interface DashboardMapper {

    // ========== 管理员首页指标 ==========

    @Select("SELECT COUNT(*) FROM student")
    int countStudent();

    @Select("SELECT COUNT(*) FROM teacher")
    int countTeacher();

    @Select("SELECT COUNT(*) FROM course")
    int countCourse();

    @Select("SELECT COUNT(*) FROM choice")
    int countChoice();

    @Select("SELECT COUNT(*) FROM apply WHERE status = '待审核'")
    int countPendingApply();

    /**
     * 今日登录次数。create_time 由数据库 now() 写入，「今天」也按数据库的日期算，两边时区一致；
     * 按区间比较而不是 DATE(create_time)，能用上 sys_login_log 的 idx_create_time。
     */
    @Select("SELECT COUNT(*) FROM sys_login_log WHERE create_time >= CURDATE() AND create_time < CURDATE() + INTERVAL 1 DAY")
    int countLoginsToday();

    // ========== 数据大屏：考勤（time 为 yyyy-MM-dd，只算学期内 [start, end) 的记录） ==========

    /** 每个教学周、每种考勤状态的次数（开学当天所在周为第 1 周） */
    @Select("<script>SELECT FLOOR(DATEDIFF(a.time, #{start}) / 7) + 1 AS week, a.status AS name, COUNT(*) AS value"
            + " FROM attendance a WHERE a.time &gt;= #{start} AND a.time &lt; #{end}"
            + "<if test='teacherId != null'> AND a.teacher_id = #{teacherId}</if>"
            + " GROUP BY week, a.status</script>")
    List<Map<String, Object>> attendanceByWeek(@Param("start") String start, @Param("end") String end,
                                               @Param("teacherId") Integer teacherId);

    /** 每门课、每种考勤状态的次数 */
    @Select("<script>SELECT a.course_id AS courseId, a.status AS name, COUNT(*) AS value"
            + " FROM attendance a WHERE a.time &gt;= #{start} AND a.time &lt; #{end}"
            + "<if test='teacherId != null'> AND a.teacher_id = #{teacherId}</if>"
            + " GROUP BY a.course_id, a.status</script>")
    List<Map<String, Object>> attendanceByCourse(@Param("start") String start, @Param("end") String end,
                                                 @Param("teacherId") Integer teacherId);

    /** 指定日期发起过签到的课程（签到已结束的也算） */
    @Select("<script>SELECT DISTINCT course_id FROM attendance_session WHERE date = #{date} AND course_id IN"
            + "<foreach collection='courseIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Integer> courseIdsWithSession(@Param("courseIds") List<Integer> courseIds, @Param("date") String date);

    // ========== 数据大屏：作业（deadline、submit_time 为 yyyy-MM-dd HH:mm） ==========

    /** 已截止作业应交的份数：每份作业按所在课程的选课人数计 */
    @Select("<script>SELECT COUNT(*) FROM assignment a JOIN choice ch ON ch.course_id = a.course_id"
            + " WHERE a.deadline &lt; #{now}<if test='teacherId != null'> AND a.teacher_id = #{teacherId}</if></script>")
    int countDueSubmissions(@Param("now") String now, @Param("teacherId") Integer teacherId);

    /** 已截止作业实际交上来的份数（只算仍在选课名单里的学生，提交率不会超过 100%） */
    @Select("<script>SELECT COUNT(*) FROM assignment a JOIN choice ch ON ch.course_id = a.course_id"
            + " JOIN homework h ON h.assignment_id = a.id AND h.student_id = ch.student_id"
            + " WHERE a.deadline &lt; #{now}<if test='teacherId != null'> AND a.teacher_id = #{teacherId}</if></script>")
    int countDueSubmitted(@Param("now") String now, @Param("teacherId") Integer teacherId);

    /** 待批改的作业（已交、还没打分） */
    @Select("<script>SELECT COUNT(*) FROM homework WHERE score IS NULL"
            + "<if test='teacherId != null'> AND teacher_id = #{teacherId}</if></script>")
    int countUngraded(@Param("teacherId") Integer teacherId);

    /** 在 before 之前交上来、到现在还没批改的作业：份数 total 与涉及的教师数 teachers */
    @Select("<script>SELECT COUNT(*) AS total, COUNT(DISTINCT teacher_id) AS teachers FROM homework"
            + " WHERE score IS NULL AND submit_time &lt; #{before}"
            + "<if test='teacherId != null'> AND teacher_id = #{teacherId}</if></script>")
    Map<String, Object> overdueUngraded(@Param("before") String before, @Param("teacherId") Integer teacherId);

    /** 教师布置的全部作业及已交份数（教师大屏挑每门课「当前作业」用） */
    @Select("SELECT a.id, a.course_id AS courseId, a.title, a.deadline,"
            + " (SELECT COUNT(*) FROM homework h WHERE h.assignment_id = a.id) AS submitted"
            + " FROM assignment a WHERE a.teacher_id = #{teacherId}")
    List<Map<String, Object>> assignmentsOfTeacher(Integer teacherId);

    /** 成绩册还是草稿的课程数 */
    @Select("<script>SELECT COUNT(DISTINCT course_id) FROM score WHERE status = '草稿'"
            + "<if test='teacherId != null'> AND teacher_id = #{teacherId}</if></script>")
    int countDraftGradebooks(@Param("teacherId") Integer teacherId);

    // ========== 数据大屏：评教 ==========

    /** 评教份数、涉及课程数与五个维度的平均分（没有评价时各平均分为 null） */
    @Select("<script>SELECT COUNT(*) AS total, COUNT(DISTINCT course_id) AS courses,"
            + " AVG(attitude) AS attitude, AVG(content_score) AS contentScore, AVG(method) AS method,"
            + " AVG(effect) AS effect, AVG(support) AS support FROM course_eval"
            + "<if test='teacherId != null'> WHERE teacher_id = #{teacherId}</if></script>")
    Map<String, Object> evaluationSummary(@Param("teacherId") Integer teacherId);

    /** 教师各门课收到的评价份数 */
    @Select("SELECT c.name AS name, COUNT(*) AS total FROM course_eval e JOIN course c ON c.id = e.course_id"
            + " WHERE e.teacher_id = #{teacherId} GROUP BY c.id, c.name ORDER BY total DESC, c.id")
    List<Map<String, Object>> evaluationByCourse(Integer teacherId);

    // ========== 数据大屏：学生与组织 ==========

    /** 各学院的学生人数（没分学院的学生 name 为空串） */
    @Select("SELECT IFNULL(co.name, '') AS name, COUNT(*) AS total FROM student s"
            + " LEFT JOIN college co ON co.id = s.college_id GROUP BY co.id, co.name")
    List<Map<String, Object>> studentsByCollege();

    /** 指定学生所在学院、班级的名称（预警按学院统计、名单显示班级用） */
    @Select("<script>SELECT s.id AS id, IFNULL(co.name, '') AS college, IFNULL(cl.name, '') AS className FROM student s"
            + " LEFT JOIN college co ON co.id = s.college_id LEFT JOIN classes cl ON cl.id = s.class_id"
            + " WHERE s.id IN<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Map<String, Object>> studentOrgs(@Param("ids") List<Integer> ids);

    /** 选了该教师任意一门课的学生人数 */
    @Select("SELECT COUNT(DISTINCT ch.student_id) FROM choice ch JOIN course c ON c.id = ch.course_id"
            + " WHERE c.teacher_id = #{teacherId}")
    int countStudentsOfTeacher(Integer teacherId);

    @Select("SELECT name, title FROM teacher WHERE id = #{id}")
    Map<String, Object> teacherBrief(Integer id);

    @Select("SELECT COUNT(*) FROM roomplan")
    int countRooms();

    /** 还没排好教室或上课时间、也没结课的课程数（口径与管理员首页的「未排课」一致） */
    @Select("SELECT COUNT(*) FROM course WHERE IFNULL(status, '') <> '已结课'"
            + " AND (IFNULL(room, '') = '' OR IFNULL(week, '') = '' OR IFNULL(segment, '') = '')")
    int countUnscheduled();

    // ========== 数据大屏：考试与通知 ==========

    /** 从 from 起（含）的考试，按时间先后，最多 limit 场 */
    @Select("SELECT id, name, exam_time AS examTime FROM examplan WHERE exam_time >= #{from}"
            + " ORDER BY exam_time, id LIMIT #{limit}")
    List<Map<String, Object>> examsFrom(@Param("from") String from, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM examplan WHERE exam_time >= #{from}")
    int countExamsFrom(String from);

    /** 最近的全校通知（课程里发的公告带 course_id，不上大屏） */
    @Select("SELECT id, title, time FROM notice WHERE course_id IS NULL ORDER BY time DESC, id DESC LIMIT #{limit}")
    List<Map<String, Object>> latestSchoolNotices(int limit);
}
