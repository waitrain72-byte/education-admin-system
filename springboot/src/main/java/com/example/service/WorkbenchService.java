package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.common.enums.SegmentEnum;
import com.example.entity.Account;
import com.example.entity.Apply;
import com.example.entity.Attendance;
import com.example.entity.Choice;
import com.example.entity.Course;
import com.example.entity.Examplan;
import com.example.entity.Notice;
import com.example.entity.Student;
import com.example.exception.CustomException;
import com.example.mapper.ApplyMapper;
import com.example.mapper.AttendanceSessionMapper;
import com.example.mapper.AttendanceMapper;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.ExamplanMapper;
import com.example.mapper.NoticeMapper;
import com.example.mapper.StudentMapper;
import com.example.mapper.WorkbenchMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 首页（工作台）聚合数据：一次请求拿齐当前角色首页要显示的内容。
 *
 * <ul>
 *   <li>学生：已选课程、今天的课、近期考试、已修学分 / 绩点 / 出勤率、学业预警；</li>
 *   <li>教师：所授课程（含选课人数、待批改作业数）、今天的课、近期考试、待办；</li>
 *   <li>管理员：关键指标、待审核请假、未排课课程、近期考试、最新通知。</li>
 * </ul>
 */
@Service
public class WorkbenchService {

    private static final Logger log = LoggerFactory.getLogger(WorkbenchService.class);

    static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter SECOND_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern SEGMENT_TIME = Pattern.compile("(\\d{1,2}:\\d{2})\\s*~\\s*(\\d{1,2}:\\d{2})");
    /** 及格线（与成绩模块一致） */
    static final double PASS_LINE = 60;
    static final String STATUS_ACTIVE = "已开课";
    static final String STATUS_FINISHED = "已结课";

    @Resource
    private ConfigService configService;
    @Resource
    private Clock clock;
    @Resource
    private ChoiceMapper choiceMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private ExamplanMapper examplanMapper;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private ApplyMapper applyMapper;
    @Resource
    private AttendanceMapper attendanceMapper;
    @Resource
    private StudentMapper studentMapper;
    @Resource
    private WorkbenchMapper workbenchMapper;
    @Resource
    private DashboardService dashboardService;
    @Resource
    private WarningService warningService;
    @Resource
    private AttendanceSessionMapper attendanceSessionMapper;

    public Map<String, Object> summary() {
        Account current = TokenUtils.getCurrentUser();
        if (current.getId() == null || StrUtil.isBlank(current.getRole())) {
            throw new CustomException(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        Map<String, Object> semester = configService.semester();
        LocalDateTime now = LocalDateTime.now(clock);
        String weekday = (String) semester.get("weekday");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("role", current.getRole());
        data.put("semester", semester);
        data.put("now", now.format(MINUTE_FORMAT));
        if (RoleEnum.STUDENT.name().equals(current.getRole())) {
            data.putAll(studentPart(current.getId(), weekday, now));
        } else if (RoleEnum.TEACHER.name().equals(current.getRole())) {
            data.putAll(teacherPart(current.getId(), weekday, now));
        } else if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            data.putAll(adminPart(now));
        }
        return data;
    }

    /**
     * 「课程」页的课程卡片：学生 = 已选的课，教师 = 自己开的课（带选课人数、待批改数），管理员 = 全部课程（带选课人数）。
     * 卡片字段与首页一致；按 未开课 / 已开课 在前、已结课在后排列。
     */
    public List<Map<String, Object>> myCourses() {
        Account current = TokenUtils.getCurrentUser();
        if (current.getId() == null || StrUtil.isBlank(current.getRole())) {
            throw new CustomException(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        List<Map<String, Object>> cards = new ArrayList<>();
        if (RoleEnum.STUDENT.name().equals(current.getRole())) {
            Choice probe = new Choice();
            probe.setStudentId(current.getId());
            for (Choice c : choiceMapper.selectAll(probe)) {
                cards.add(courseCard(c.getCourseId(), c.getName(), c.getType(), c.getScore(), c.getTeacherName(),
                        c.getRoom(), c.getWeek(), c.getSegment(), c.getStatus(), c.getNum()));
            }
        } else {
            boolean teacher = RoleEnum.TEACHER.name().equals(current.getRole());
            Course probe = new Course();
            if (teacher) {
                probe.setTeacherId(current.getId());
            }
            Map<Integer, Long> students = countMap(teacher
                    ? workbenchMapper.countStudentsByTeacher(current.getId())
                    : workbenchMapper.countStudentsAll());
            Map<Integer, Long> ungraded = teacher
                    ? countMap(workbenchMapper.countUngradedByTeacher(current.getId()))
                    : new HashMap<>();
            for (Course c : courseMapper.selectAll(probe)) {
                Map<String, Object> card = courseCard(c.getId(), c.getName(), c.getType(), c.getScore(),
                        c.getTeacherName(), c.getRoom(), c.getWeek(), c.getSegment(), c.getStatus(), c.getNum());
                card.put("studentCount", students.getOrDefault(c.getId(), 0L));
                if (teacher) {
                    card.put("ungraded", ungraded.getOrDefault(c.getId(), 0L));
                }
                cards.add(card);
            }
        }
        markSigning(cards, LocalDateTime.now(clock));
        cards.sort(Comparator.comparingInt(c -> STATUS_FINISHED.equals(c.get("status")) ? 1 : 0));
        return cards;
    }

    private Map<String, Object> studentPart(Integer studentId, String weekday, LocalDateTime now) {
        Choice probe = new Choice();
        probe.setStudentId(studentId);
        List<Map<String, Object>> courses = new ArrayList<>();
        for (Choice c : choiceMapper.selectAll(probe)) {
            courses.add(courseCard(c.getCourseId(), c.getName(), c.getType(), c.getScore(), c.getTeacherName(),
                    c.getRoom(), c.getWeek(), c.getSegment(), c.getStatus(), c.getNum()));
        }

        markSigning(courses, now);

        Map<String, Object> part = new LinkedHashMap<>();
        part.put("courses", courses);
        part.put("todayCourses", todayCourses(courses, weekday));
        part.put("exams", upcomingExams(now, 3));
        part.put("todos", pendingAssignments(studentId, now));

        Student student = studentMapper.selectById(studentId);
        Attendance attendanceProbe = new Attendance();
        attendanceProbe.setStudentId(studentId);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("credits", student == null || student.getScore() == null ? 0 : student.getScore());
        stats.put("requiredCredits", workbenchMapper.requiredCredits(studentId));
        stats.put("gpa", gpa(workbenchMapper.scoresWithCredit(studentId)));
        stats.put("attendanceRate", attendanceRate(attendanceMapper.selectStatusDistribution(attendanceProbe)));
        stats.put("ongoingCourses", courses.stream().filter(c -> !STATUS_FINISHED.equals(c.get("status"))).count());
        part.put("stats", stats);

        part.put("warning", warningOf(studentId));
        return part;
    }

    private Map<String, Object> teacherPart(Integer teacherId, String weekday, LocalDateTime now) {
        Course probe = new Course();
        probe.setTeacherId(teacherId);
        Map<Integer, Long> students = countMap(workbenchMapper.countStudentsByTeacher(teacherId));
        Map<Integer, Long> ungraded = countMap(workbenchMapper.countUngradedByTeacher(teacherId));

        List<Map<String, Object>> courses = new ArrayList<>();
        List<Map<String, Object>> todos = new ArrayList<>();
        for (Course c : courseMapper.selectAll(probe)) {
            Map<String, Object> card = courseCard(c.getId(), c.getName(), c.getType(), c.getScore(), c.getTeacherName(),
                    c.getRoom(), c.getWeek(), c.getSegment(), c.getStatus(), c.getNum());
            long ungradedCount = ungraded.getOrDefault(c.getId(), 0L);
            card.put("studentCount", students.getOrDefault(c.getId(), 0L));
            card.put("ungraded", ungradedCount);
            courses.add(card);
            if (ungradedCount > 0) {
                Map<String, Object> todo = new LinkedHashMap<>();
                todo.put("type", "grading");
                todo.put("courseId", c.getId());
                todo.put("courseName", c.getName());
                todo.put("count", ungradedCount);
                todos.add(todo);
            }
        }

        markSigning(courses, now);

        Map<String, Object> part = new LinkedHashMap<>();
        part.put("courses", courses);
        part.put("todayCourses", todayCourses(courses, weekday));
        part.put("exams", upcomingExams(now, 3));
        part.put("todos", todos);
        part.put("warningCount", warningCount());
        return part;
    }

    private Map<String, Object> adminPart(LocalDateTime now) {
        Map<String, Object> stats = dashboardService.stats();
        Map<String, Object> kpis = new LinkedHashMap<>();
        for (String key : new String[]{"studentCount", "teacherCount", "courseCount", "choiceCount",
                "pendingApply", "ungradedHomework", "loginToday"}) {
            kpis.put(key, stats.get(key));
        }
        kpis.put("activeCourseCount", workbenchMapper.countActiveCourses());

        Apply pending = new Apply();
        pending.setStatus("待审核");
        PageHelper.startPage(1, 5, false);
        List<Apply> pendingApplies = applyMapper.selectAll(pending);

        PageHelper.startPage(1, 5, false);
        List<Notice> notices = noticeMapper.selectAll(new Notice());

        Map<String, Object> part = new LinkedHashMap<>();
        part.put("kpis", kpis);
        part.put("pendingApplies", pendingApplies);
        part.put("unscheduledCourses", workbenchMapper.unscheduledCourses());
        part.put("exams", upcomingExams(now, 5));
        part.put("notices", notices);
        return part;
    }

    /** 课程卡片：前端首页、课程列表共用同一套字段 */
    static Map<String, Object> courseCard(Integer id, String name, String type, Integer credit, String teacherName,
                                          String room, String week, String segment, String status, Integer capacity) {
        String[] times = segmentTimes(segment);
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", id);
        card.put("name", name);
        card.put("type", type);
        card.put("credit", credit);
        card.put("teacherName", teacherName);
        card.put("room", room);
        card.put("week", week);
        card.put("segment", segment);
        card.put("start", times[0]);
        card.put("end", times[1]);
        card.put("status", status);
        card.put("capacity", capacity);
        return card;
    }

    /** 今天要上的课：正在开课、星期对得上，按节次先后排序 */
    static List<Map<String, Object>> todayCourses(List<Map<String, Object>> courses, String weekday) {
        return courses.stream()
                .filter(c -> STATUS_ACTIVE.equals(c.get("status")) && weekday != null && weekday.equals(c.get("week")))
                .sorted(Comparator.comparingInt(c -> segmentOrder((String) c.get("segment"))))
                .collect(Collectors.toList());
    }

    /** 从「第一大节（08:30 ~ 10:10）」解析出 [08:30, 10:10]；解析不到返回两个空串 */
    static String[] segmentTimes(String segment) {
        if (segment != null) {
            Matcher m = SEGMENT_TIME.matcher(segment);
            if (m.find()) {
                return new String[]{pad(m.group(1)), pad(m.group(2))};
            }
        }
        return new String[]{"", ""};
    }

    /** 节次先后：按 SegmentEnum 顺序，未知节次排最后 */
    static int segmentOrder(String segment) {
        SegmentEnum[] values = SegmentEnum.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].segment.equals(segment)) {
                return i;
            }
        }
        return values.length;
    }

    /**
     * 平均学分绩点（成绩单也用这一个）：及格时 绩点 =（成绩 − 50）÷ 10，上限 5.0，不及格记 0，按学分加权。
     * 没有学分可加权的成绩时返回 null。
     */
    static Double gpa(List<Map<String, Object>> scoresWithCredit) {
        double creditSum = 0;
        double pointSum = 0;
        for (Map<String, Object> row : scoresWithCredit) {
            Number score = (Number) row.get("score");
            Number credit = (Number) row.get("credit");
            if (score == null || credit == null || credit.doubleValue() <= 0) {
                continue;
            }
            creditSum += credit.doubleValue();
            pointSum += gradePoint(score.doubleValue()) * credit.doubleValue();
        }
        if (creditSum == 0) {
            return null;
        }
        return Math.round(pointSum / creditSum * 100) / 100.0;
    }

    static double gradePoint(double score) {
        if (score < PASS_LINE) {
            return 0;
        }
        return Math.min(5, (score - 50) / 10);
    }

    /**
     * 出勤率（百分比，一位小数）：迟到、早退算出勤，缺勤不算；请假不计入分母。没有考勤记录时返回 null。
     */
    static Double attendanceRate(List<Map<String, Object>> statusCounts) {
        long total = 0;
        long absent = 0;
        long leave = 0;
        for (Map<String, Object> row : statusCounts) {
            Object value = row.get("value");
            long count = value instanceof Number ? ((Number) value).longValue() : 0;
            String status = String.valueOf(row.get("name"));
            total += count;
            if ("缺勤".equals(status)) {
                absent += count;
            } else if ("请假".equals(status)) {
                leave += count;
            }
        }
        long counted = total - leave;
        if (counted <= 0) {
            return null;
        }
        return Math.round((counted - absent) * 1000.0 / counted) / 10.0;
    }

    /** 未来（含此刻）的考试，按考试时间由近到远；没填考试时间的不算 */
    private List<Examplan> upcomingExams(LocalDateTime now, int limit) {
        String nowText = now.format(MINUTE_FORMAT);
        return examplanMapper.selectAll(new Examplan()).stream()
                .filter(e -> StrUtil.isNotBlank(e.getExamTime()) && e.getExamTime().compareTo(nowText) >= 0)
                .sorted(Comparator.comparing(Examplan::getExamTime))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** 给课程卡片标上「正在签到」（有进行中、没过截止时间的签到） */
    private void markSigning(List<Map<String, Object>> cards, LocalDateTime now) {
        List<Integer> ids = new ArrayList<>();
        for (Map<String, Object> card : cards) {
            if (card.get("id") instanceof Integer) {
                ids.add((Integer) card.get("id"));
            }
        }
        Set<Integer> signing = Collections.emptySet();
        if (!ids.isEmpty()) {
            try {
                signing = new HashSet<>(attendanceSessionMapper.selectSigningCourseIds(ids, now.format(SECOND_FORMAT)));
            } catch (Exception e) {
                log.warn("首页签到状态查询失败：{}", e.getMessage());
            }
        }
        for (Map<String, Object> card : cards) {
            card.put("signing", signing.contains(card.get("id")));
        }
    }

    /** 学生还没交、没过截止的作业（最多 5 条，截止早的在前） */
    private List<Map<String, Object>> pendingAssignments(Integer studentId, LocalDateTime now) {
        try {
            return workbenchMapper.pendingAssignments(studentId, now.format(MINUTE_FORMAT));
        } catch (Exception e) {
            log.warn("首页待交作业查询失败：{}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 学生本人的学业预警；正常或没有数据时返回 null（首页不显示预警条） */
    private Map<String, Object> warningOf(Integer studentId) {
        try {
            Map<String, Object> row = warningService.warningOf(studentId);
            if (row == null || "正常".equals(row.get("level"))) {
                return null;
            }
            Map<String, Object> warning = new LinkedHashMap<>();
            warning.put("level", row.get("level"));
            warning.put("riskIndex", row.get("riskIndex"));
            warning.put("suggestion", row.get("suggestion"));
            return warning;
        } catch (Exception e) {
            log.warn("首页学业预警计算失败：{}", e.getMessage());
            return null;
        }
    }

    /** 教师名下需要关注的学生数（预警等级不是「正常」的） */
    private long warningCount() {
        try {
            return warningService.listWarnings().stream().filter(row -> !"正常".equals(row.get("level"))).count();
        } catch (Exception e) {
            log.warn("首页预警人数统计失败：{}", e.getMessage());
            return 0;
        }
    }

    private static Map<Integer, Long> countMap(List<Map<String, Object>> rows) {
        Map<Integer, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get("courseId");
            Object total = row.get("total");
            if (id instanceof Number && total instanceof Number) {
                map.put(((Number) id).intValue(), ((Number) total).longValue());
            }
        }
        return map;
    }

    private static String pad(String hhmm) {
        return hhmm.length() == 4 ? "0" + hhmm : hhmm;
    }
}
