package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.AppTime;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.common.enums.SegmentEnum;
import com.example.common.enums.WeekEnum;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.exception.CustomException;
import com.example.mapper.CourseMapper;
import com.example.mapper.DashboardMapper;
import com.example.mapper.WorkbenchMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 管理员首页指标与数据大屏。
 *
 * <p>数据大屏只给管理员和教师看：管理员看全校，教师只看本人任课的课程与学生；学生即使在权限设置里
 * 被勾上「数据大屏」也一律拒绝。结果按「谁在看」分别缓存 {@link #CACHE_MILLIS} 毫秒（前端每分钟刷新一次），
 * 窗口内的请求共享同一份结果。单实例部署适用，多实例需换成 Redis 等外置缓存。</p>
 *
 * <p>展示按「全校几千学生、几百门课」也能看的思路组织：名单只给前几条并附总数，比例同时给出分子分母，
 * 选课人数太少的课不参与出勤排名，课程公告不上大屏，考试只列近期几场。</p>
 */
@Service
public class DashboardService {

    /** 缓存有效期：大屏每分钟刷新一次，30 秒的新鲜度足够 */
    private static final long CACHE_MILLIS = 30 * 1000L;

    private static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 选课少于这么多人的课不参与出勤排名（人太少，比例大起大落） */
    static final int MIN_RANKED_STUDENTS = 5;
    /** 出勤率最低的课最多列几门 */
    static final int LOWEST_COURSES = 5;
    /** 出勤率分档：≥95% 为好、90%–95% 一般、<90% 需要关注 */
    static final double RATE_GOOD = 95;
    static final double RATE_FAIR = 90;
    /** 预警名单、需要注意的课最多列几条；名单里点名的课程最多几门 */
    static final int TOP_ITEMS = 3;
    /** 按学院统计预警时最多列几个学院 */
    static final int TOP_COLLEGES = 5;
    /** 上课超过这么多分钟还没发起签到，才算需要注意 */
    static final int CHECK_IN_GRACE_MINUTES = 5;
    /** 交上来超过这么多天还没批改，算拖得太久 */
    static final int OVERDUE_GRADING_DAYS = 7;
    /** 考试只列这么多天以内的，最多几场 */
    static final int EXAM_WINDOW_DAYS = 30;
    static final int EXAM_LIMIT = 3;
    static final int NOTICE_LIMIT = 3;

    static final String STATUS_ACTIVE = "已开课";
    static final String LEVEL_HIGH = "高风险";
    static final String LEVEL_MIDDLE = "中风险";
    static final String LEVEL_LOW = "低风险";

    @Resource
    private DashboardMapper dashboardMapper;
    @Resource
    private WorkbenchMapper workbenchMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private ConfigService configService;
    @Resource
    private WarningService warningService;

    private final AtomicReference<Cached> kpiCache = new AtomicReference<>();
    private final Map<String, Cached> screenCache = new ConcurrentHashMap<>();

    private static class Cached {
        final Map<String, Object> data;
        final long expireAt;

        Cached(Map<String, Object> data, long expireAt) {
            this.data = data;
            this.expireAt = expireAt;
        }
    }

    /** 管理员首页的关键指标（与大屏分开缓存）；返回不可变视图，避免调用方误改缓存 */
    public Map<String, Object> kpis() {
        Cached cached = kpiCache.get();
        long now = System.currentTimeMillis();
        if (cached != null && cached.expireAt > now) {
            return cached.data;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentCount", dashboardMapper.countStudent());
        data.put("teacherCount", dashboardMapper.countTeacher());
        data.put("courseCount", dashboardMapper.countCourse());
        data.put("choiceCount", dashboardMapper.countChoice());
        data.put("pendingApply", dashboardMapper.countPendingApply());
        data.put("ungradedHomework", dashboardMapper.countUngraded(null));
        data.put("loginToday", dashboardMapper.countLoginsToday());
        Map<String, Object> view = Collections.unmodifiableMap(data);
        kpiCache.set(new Cached(view, now + CACHE_MILLIS));
        return view;
    }

    /** 数据大屏：管理员看全校，教师只看本人任课的数据 */
    public Map<String, Object> screen() {
        Account current = TokenUtils.getCurrentUser();
        Integer teacherId = viewerTeacherId(current);
        String key = teacherId == null ? "ADMIN" : "TEACHER-" + teacherId;
        long now = System.currentTimeMillis();
        Cached cached = screenCache.get(key);
        if (cached != null && cached.expireAt > now) {
            return cached.data;
        }
        // 顺手清掉过期的，教师多了缓存也不会只增不减
        screenCache.values().removeIf(c -> c.expireAt <= now);
        Map<String, Object> data = Collections.unmodifiableMap(loadScreen(current.getRole(), teacherId));
        screenCache.put(key, new Cached(data, now + CACHE_MILLIS));
        return data;
    }

    /** 管理员返回 null（看全校），教师返回本人 id；其余身份一律拒绝 */
    static Integer viewerTeacherId(Account current) {
        if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            return null;
        }
        if (RoleEnum.TEACHER.name().equals(current.getRole()) && current.getId() != null) {
            return current.getId();
        }
        throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
    }

    private Map<String, Object> loadScreen(String role, Integer teacherId) {
        LocalDateTime now = LocalDateTime.now(AppTime.clock());
        Map<String, Object> semester = configService.semester();

        Course probe = new Course();
        probe.setTeacherId(teacherId);
        List<Course> courses = courseMapper.selectAll(probe);
        Map<Integer, Long> enrolled = WorkbenchService.countMap(teacherId == null
                ? workbenchMapper.countStudentsAll()
                : workbenchMapper.countStudentsByTeacher(teacherId));
        long students = teacherId == null ? dashboardMapper.countStudent() : dashboardMapper.countStudentsOfTeacher(teacherId);

        // 考勤只算本学期（开学日期起、教学周数以内）；开学日期没配好时不出考勤数据
        List<Map<String, Object>> weekRows = Collections.emptyList();
        List<Map<String, Object>> courseRows = Collections.emptyList();
        LocalDate start = ConfigService.parseDate(String.valueOf(semester.get("startDate")));
        if (start != null) {
            int weeks = semester.get("weeks") instanceof Number ? ((Number) semester.get("weeks")).intValue() : 0;
            String from = start.toString();
            String to = start.plusWeeks(weeks).toString();
            weekRows = dashboardMapper.attendanceByWeek(from, to, teacherId);
            courseRows = dashboardMapper.attendanceByCourse(from, to, teacherId);
        }
        Map<Integer, long[]> perCourse = attendanceByCourse(courseRows);

        Map<String, Object> assignments = assignmentSection(now, teacherId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("role", role);
        data.put("now", now.format(MINUTE_FORMAT));
        data.put("semester", semester);
        if (teacherId != null) {
            Map<String, Object> brief = dashboardMapper.teacherBrief(teacherId);
            Map<String, Object> teacher = new LinkedHashMap<>();
            teacher.put("name", brief == null ? "" : brief.get("name"));
            teacher.put("title", brief == null ? "" : brief.get("title"));
            teacher.put("courseCount", courses.size());
            teacher.put("studentCount", students);
            data.put("teacher", teacher);
        }

        Map<String, Object> attendance = new LinkedHashMap<>();
        attendance.put("weekly", weeklyRates(weekRows));
        if (teacherId == null) {
            attendance.put("ranking", courseRanking(courses, enrolled, perCourse));
        } else {
            attendance.put("courses", teacherCourses(courses, enrolled, perCourse,
                    dashboardMapper.assignmentsOfTeacher(teacherId), now));
        }
        data.put("attendance", attendance);
        data.put("assignments", assignments);
        data.put("evaluation", evaluationSection(teacherId));
        data.put("warnings", warningSection(teacherId, students));
        data.put("today", todaySection(courses, enrolled, (String) semester.get("weekday"), now, teacherId != null));
        data.put("week", weekSection(courses, enrolled));
        data.put("exams", examSection(now));
        data.put("notices", dashboardMapper.latestSchoolNotices(NOTICE_LIMIT));

        Map<String, Object> todos = new LinkedHashMap<>();
        if (teacherId == null) {
            todos.put("pendingApply", dashboardMapper.countPendingApply());
        }
        todos.put("ungraded", assignments.get("ungraded"));
        todos.put("draftGradebooks", dashboardMapper.countDraftGradebooks(teacherId));
        if (teacherId == null) {
            todos.put("unscheduled", dashboardMapper.countUnscheduled());
        }
        data.put("todos", todos);
        return data;
    }

    // ==================== 考勤 ====================

    /** 每周出勤率（按周次先后，只给有考勤记录的周） */
    static List<Map<String, Object>> weeklyRates(List<Map<String, Object>> rows) {
        Map<Integer, List<Map<String, Object>>> byWeek = new TreeMap<>();
        for (Map<String, Object> row : rows) {
            if (row.get("week") instanceof Number) {
                byWeek.computeIfAbsent(((Number) row.get("week")).intValue(), k -> new ArrayList<>()).add(row);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, List<Map<String, Object>>> entry : byWeek.entrySet()) {
            long[] counts = WorkbenchService.attendanceCounts(entry.getValue());
            if (counts[1] <= 0) {
                continue;
            }
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("week", entry.getKey());
            point.put("rate", WorkbenchService.percent(counts[0], counts[1]));
            point.put("present", counts[0]);
            point.put("counted", counts[1]);
            result.add(point);
        }
        return result;
    }

    /** 课程 id → [出勤次数, 计入的考勤次数] */
    static Map<Integer, long[]> attendanceByCourse(List<Map<String, Object>> rows) {
        Map<Integer, List<Map<String, Object>>> grouped = new HashMap<>();
        for (Map<String, Object> row : rows) {
            if (row.get("courseId") instanceof Number) {
                grouped.computeIfAbsent(((Number) row.get("courseId")).intValue(), k -> new ArrayList<>()).add(row);
            }
        }
        Map<Integer, long[]> result = new HashMap<>();
        grouped.forEach((id, list) -> result.put(id, WorkbenchService.attendanceCounts(list)));
        return result;
    }

    /**
     * 全校课程的出勤排名：只列出勤率最低的几门，另给三档的门数；
     * 选课人数不足 {@link #MIN_RANKED_STUDENTS} 的课不参与排名，正在开课却没有考勤记录的课单独计数。
     */
    static Map<String, Object> courseRanking(List<Course> courses, Map<Integer, Long> enrolled, Map<Integer, long[]> perCourse) {
        List<Map<String, Object>> ranked = new ArrayList<>();
        int good = 0;
        int fair = 0;
        int low = 0;
        List<String> small = new ArrayList<>();
        List<String> noRecord = new ArrayList<>();
        for (Course c : courses) {
            long[] counts = perCourse.get(c.getId());
            if (counts == null || counts[1] <= 0) {
                if (STATUS_ACTIVE.equals(c.getStatus())) {
                    noRecord.add(c.getName());
                }
                continue;
            }
            long students = enrolled.getOrDefault(c.getId(), 0L);
            if (students < MIN_RANKED_STUDENTS) {
                small.add(c.getName());
                continue;
            }
            double rate = WorkbenchService.percent(counts[0], counts[1]);
            if (rate >= RATE_GOOD) {
                good++;
            } else if (rate >= RATE_FAIR) {
                fair++;
            } else {
                low++;
            }
            ranked.add(courseRate(c, students, counts));
        }
        ranked.sort(Comparator.comparingDouble((Map<String, Object> m) -> (Double) m.get("rate")));

        Map<String, Object> bands = new LinkedHashMap<>();
        bands.put("good", good);
        bands.put("fair", fair);
        bands.put("low", low);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("limit", LOWEST_COURSES);
        result.put("minStudents", MIN_RANKED_STUDENTS);
        result.put("bands", bands);
        result.put("lowest", new ArrayList<>(ranked.subList(0, Math.min(LOWEST_COURSES, ranked.size()))));
        result.put("smallCount", small.size());
        result.put("smallNames", sample(small));
        result.put("noRecordCount", noRecord.size());
        result.put("noRecordNames", sample(noRecord));
        return result;
    }

    /**
     * 教师本人正在开课的每门课：出勤率与「当前作业」。
     * 当前作业取截止时间还没到的里面最早的一份；都截止了取最近截止的一份。
     */
    static List<Map<String, Object>> teacherCourses(List<Course> courses, Map<Integer, Long> enrolled,
                                                    Map<Integer, long[]> perCourse,
                                                    List<Map<String, Object>> assignments, LocalDateTime now) {
        String nowText = now.format(MINUTE_FORMAT);
        Map<Integer, List<Map<String, Object>>> byCourse = new HashMap<>();
        for (Map<String, Object> a : assignments) {
            if (a.get("courseId") instanceof Number && a.get("deadline") != null) {
                byCourse.computeIfAbsent(((Number) a.get("courseId")).intValue(), k -> new ArrayList<>()).add(a);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Course c : courses) {
            if (!STATUS_ACTIVE.equals(c.getStatus())) {
                continue;
            }
            long students = enrolled.getOrDefault(c.getId(), 0L);
            long[] counts = perCourse.getOrDefault(c.getId(), new long[]{0, 0});
            Map<String, Object> row = courseRate(c, students, counts);
            Map<String, Object> current = currentAssignment(byCourse.getOrDefault(c.getId(), Collections.emptyList()), nowText);
            if (current != null) {
                Map<String, Object> assignment = new LinkedHashMap<>();
                assignment.put("title", current.get("title"));
                assignment.put("deadline", current.get("deadline"));
                assignment.put("submitted", num(current.get("submitted")));
                assignment.put("students", students);
                assignment.put("closed", String.valueOf(current.get("deadline")).compareTo(nowText) < 0);
                row.put("assignment", assignment);
            }
            result.add(row);
        }
        return result;
    }

    static Map<String, Object> currentAssignment(List<Map<String, Object>> list, String nowText) {
        Map<String, Object> upcoming = null;
        Map<String, Object> latestClosed = null;
        for (Map<String, Object> a : list) {
            String deadline = String.valueOf(a.get("deadline"));
            if (deadline.compareTo(nowText) >= 0) {
                if (upcoming == null || deadline.compareTo(String.valueOf(upcoming.get("deadline"))) < 0) {
                    upcoming = a;
                }
            } else if (latestClosed == null || deadline.compareTo(String.valueOf(latestClosed.get("deadline"))) > 0) {
                latestClosed = a;
            }
        }
        return upcoming != null ? upcoming : latestClosed;
    }

    private static Map<String, Object> courseRate(Course c, long students, long[] counts) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", c.getId());
        row.put("name", c.getName());
        row.put("teacherName", c.getTeacherName());
        row.put("students", students);
        row.put("rate", WorkbenchService.percent(counts[0], counts[1]));
        row.put("present", counts[0]);
        row.put("counted", counts[1]);
        return row;
    }

    // ==================== 作业、评教 ====================

    private Map<String, Object> assignmentSection(LocalDateTime now, Integer teacherId) {
        String nowText = now.format(MINUTE_FORMAT);
        Map<String, Object> overdue = dashboardMapper.overdueUngraded(
                now.minusDays(OVERDUE_GRADING_DAYS).format(MINUTE_FORMAT), teacherId);
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("due", dashboardMapper.countDueSubmissions(nowText, teacherId));
        section.put("submitted", dashboardMapper.countDueSubmitted(nowText, teacherId));
        section.put("ungraded", dashboardMapper.countUngraded(teacherId));
        section.put("overdue", overdue == null ? 0 : num(overdue.get("total")));
        section.put("overdueTeachers", overdue == null ? 0 : num(overdue.get("teachers")));
        section.put("overdueDays", OVERDUE_GRADING_DAYS);
        return section;
    }

    private Map<String, Object> evaluationSection(Integer teacherId) {
        Map<String, Object> row = dashboardMapper.evaluationSummary(teacherId);
        if (row == null) {
            row = Collections.emptyMap();
        }
        Map<String, Object> dimensions = new LinkedHashMap<>();
        double sum = 0;
        int n = 0;
        for (String key : new String[]{"attitude", "contentScore", "method", "effect", "support"}) {
            Double value = round2(row.get(key));
            dimensions.put(key, value);
            if (value != null) {
                sum += value;
                n++;
            }
        }
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("total", num(row.get("total")));
        section.put("courses", num(row.get("courses")));
        section.put("average", n == 0 ? null : round2(sum / n));
        section.put("dimensions", dimensions);
        if (teacherId != null) {
            section.put("byCourse", dashboardMapper.evaluationByCourse(teacherId));
        }
        return section;
    }

    // ==================== 学业预警 ====================

    /**
     * 预警：分级人数 + 风险最高的几名（管理员另给按学院的分布）。
     * 计算沿用 {@link WarningService#listWarnings()}，范围随当前登录人（管理员全校、教师本人任课）。
     */
    private Map<String, Object> warningSection(Integer teacherId, long students) {
        List<Map<String, Object>> warned = warningService.listWarnings().stream()
                .filter(row -> isWarned(row.get("level")))
                .collect(Collectors.toList());
        long high = warned.stream().filter(row -> LEVEL_HIGH.equals(row.get("level"))).count();
        long middle = warned.stream().filter(row -> LEVEL_MIDDLE.equals(row.get("level"))).count();
        long low = warned.size() - high - middle;

        // 只为要显示的学生查学院、班级；管理员按学院统计时需要全部预警学生的学院
        List<Map<String, Object>> top = warned.subList(0, Math.min(TOP_ITEMS, warned.size()));
        List<Integer> ids = (teacherId == null ? warned : top).stream()
                .map(row -> row.get("studentId")).filter(id -> id instanceof Integer).map(id -> (Integer) id)
                .collect(Collectors.toList());
        Map<Integer, Map<String, Object>> orgs = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Map<String, Object> org : dashboardMapper.studentOrgs(ids)) {
                if (org.get("id") instanceof Number) {
                    orgs.put(((Number) org.get("id")).intValue(), org);
                }
            }
        }

        List<Map<String, Object>> topRows = new ArrayList<>();
        for (Map<String, Object> row : top) {
            Map<String, Object> org = orgs.getOrDefault(row.get("studentId"), Collections.emptyMap());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("studentId", row.get("studentId"));
            item.put("name", row.get("studentName"));
            item.put("className", org.getOrDefault("className", ""));
            item.put("level", row.get("level"));
            item.put("riskIndex", row.get("riskIndex"));
            item.put("avgScore", row.get("avgScore"));
            item.put("failedCount", row.get("failedCount"));
            item.put("absentRate", row.get("absentRate"));
            topRows.add(item);
        }

        Map<String, Object> section = new LinkedHashMap<>();
        section.put("students", students);
        section.put("high", high);
        section.put("middle", middle);
        section.put("low", low);
        section.put("normal", Math.max(0, students - warned.size()));
        section.put("top", topRows);
        if (teacherId == null) {
            section.put("byCollege", collegeBreakdown(warned, orgs, dashboardMapper.studentsByCollege()));
        }
        return section;
    }

    static boolean isWarned(Object level) {
        return LEVEL_HIGH.equals(level) || LEVEL_MIDDLE.equals(level) || LEVEL_LOW.equals(level);
    }

    /** 各学院的预警人数 / 学院人数，预警多的在前，最多 {@link #TOP_COLLEGES} 个（没分学院的 name 为空串） */
    static List<Map<String, Object>> collegeBreakdown(List<Map<String, Object>> warned,
                                                     Map<Integer, Map<String, Object>> orgs,
                                                     List<Map<String, Object>> totals) {
        Map<String, Long> warnedByCollege = new HashMap<>();
        for (Map<String, Object> row : warned) {
            Map<String, Object> org = orgs.getOrDefault(row.get("studentId"), Collections.emptyMap());
            warnedByCollege.merge(String.valueOf(org.getOrDefault("college", "")), 1L, Long::sum);
        }
        Map<String, Long> totalByCollege = new HashMap<>();
        for (Map<String, Object> row : totals) {
            totalByCollege.merge(String.valueOf(row.get("name")), num(row.get("total")), Long::sum);
        }
        return warnedByCollege.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.<String, Long>comparingByKey()))
                .limit(TOP_COLLEGES)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("warned", e.getValue());
                    item.put("total", totalByCollege.getOrDefault(e.getKey(), e.getValue()));
                    return item;
                })
                .collect(Collectors.toList());
    }

    // ==================== 今天、本周 ====================

    /**
     * 今天的课按节次汇总：每节几门课、应到多少人、占几间教室、这节是否已过；
     * 再挑出「上课超过几分钟还没发起签到」的课和同一时间同一教室排了两门课的冲突。
     * 教师大屏另给今天每门课的明细（教师一天就几节课）。
     */
    private Map<String, Object> todaySection(List<Course> courses, Map<Integer, Long> enrolled, String weekday,
                                             LocalDateTime now, boolean withCourses) {
        List<Course> today = courses.stream()
                .filter(c -> STATUS_ACTIVE.equals(c.getStatus()) && weekday != null && weekday.equals(c.getWeek()))
                .sorted(Comparator.comparingInt(c -> WorkbenchService.segmentOrder(c.getSegment())))
                .collect(Collectors.toList());
        List<Integer> ids = today.stream().map(Course::getId).collect(Collectors.toList());
        Set<Integer> checkedIn = ids.isEmpty() ? Collections.emptySet()
                : new HashSet<>(dashboardMapper.courseIdsWithSession(ids, now.toLocalDate().toString()));
        LocalTime time = now.toLocalTime();

        List<Map<String, Object>> periods = new ArrayList<>();
        SegmentEnum[] segments = SegmentEnum.values();
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].segment;
            String[] times = WorkbenchService.segmentTimes(segment);
            List<Course> inPeriod = today.stream().filter(c -> segment.equals(c.getSegment())).collect(Collectors.toList());
            Map<String, Object> period = new LinkedHashMap<>();
            period.put("index", i + 1);
            period.put("start", times[0]);
            period.put("end", times[1]);
            period.put("state", phase(time, times[0], times[1]));
            period.put("courses", inPeriod.size());
            period.put("students", inPeriod.stream().mapToLong(c -> enrolled.getOrDefault(c.getId(), 0L)).sum());
            period.put("rooms", inPeriod.stream().map(Course::getRoom).filter(StrUtil::isNotBlank).distinct().count());
            periods.add(period);
        }

        List<Map<String, Object>> attention = new ArrayList<>();
        Map<String, Integer> slots = new HashMap<>();
        List<Map<String, Object>> cards = new ArrayList<>();
        for (Course c : today) {
            String[] times = WorkbenchService.segmentTimes(c.getSegment());
            String state = phase(time, times[0], times[1]);
            if ("ongoing".equals(state) && !checkedIn.contains(c.getId())) {
                long minutes = minutesSince(time, times[0]);
                if (minutes >= CHECK_IN_GRACE_MINUTES) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("courseId", c.getId());
                    item.put("courseName", c.getName());
                    item.put("room", c.getRoom());
                    item.put("teacherName", c.getTeacherName());
                    item.put("minutes", minutes);
                    attention.add(item);
                }
            }
            if (StrUtil.isNotBlank(c.getRoom())) {
                slots.merge(c.getRoom() + "|" + c.getSegment(), 1, Integer::sum);
            }
            if (withCourses) {
                Map<String, Object> card = new LinkedHashMap<>();
                card.put("id", c.getId());
                card.put("name", c.getName());
                card.put("room", c.getRoom());
                card.put("start", times[0]);
                card.put("end", times[1]);
                card.put("period", WorkbenchService.segmentOrder(c.getSegment()) + 1);
                card.put("students", enrolled.getOrDefault(c.getId(), 0L));
                card.put("state", state);
                card.put("checkedIn", checkedIn.contains(c.getId()));
                cards.add(card);
            }
        }
        attention.sort(Comparator.comparingLong((Map<String, Object> m) -> (Long) m.get("minutes")).reversed());

        Map<String, Object> section = new LinkedHashMap<>();
        section.put("courses", today.size());
        section.put("students", today.stream().mapToLong(c -> enrolled.getOrDefault(c.getId(), 0L)).sum());
        section.put("roomsUsed", today.stream().map(Course::getRoom).filter(StrUtil::isNotBlank).distinct().count());
        section.put("roomsTotal", dashboardMapper.countRooms());
        section.put("periods", periods);
        section.put("attentionCount", attention.size());
        section.put("attention", new ArrayList<>(attention.subList(0, Math.min(TOP_ITEMS, attention.size()))));
        section.put("conflicts", slots.values().stream().filter(n -> n > 1).count());
        if (withCourses) {
            section.put("list", cards);
        }
        return section;
    }

    /** 这一节相对现在：finished 已过 / ongoing 正在上 / upcoming 还没到（时间解析不了按还没到算） */
    static String phase(LocalTime now, String start, String end) {
        LocalTime from = parseTime(start);
        LocalTime to = parseTime(end);
        if (from == null || to == null || now.isBefore(from)) {
            return "upcoming";
        }
        return now.isAfter(to) ? "finished" : "ongoing";
    }

    static long minutesSince(LocalTime now, String start) {
        LocalTime from = parseTime(start);
        return from == null ? 0 : ChronoUnit.MINUTES.between(from, now);
    }

    /**
     * 本周排课：一周七天 × 五个大节，每格是正在开课的课程数和课程名单。
     * 课表每周重复，按课程的星期、节次统计即可。
     */
    static List<Map<String, Object>> weekSection(List<Course> courses, Map<Integer, Long> enrolled) {
        List<Map<String, Object>> days = new ArrayList<>();
        WeekEnum[] weekdays = WeekEnum.values();
        for (int d = 0; d < weekdays.length; d++) {
            String weekday = weekdays[d].week;
            List<Map<String, Object>> cells = new ArrayList<>();
            int total = 0;
            for (SegmentEnum segment : SegmentEnum.values()) {
                List<Map<String, Object>> names = new ArrayList<>();
                for (Course c : courses) {
                    if (STATUS_ACTIVE.equals(c.getStatus()) && weekday.equals(c.getWeek()) && segment.segment.equals(c.getSegment())) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("name", c.getName());
                        item.put("room", c.getRoom());
                        item.put("teacherName", c.getTeacherName());
                        item.put("students", enrolled.getOrDefault(c.getId(), 0L));
                        names.add(item);
                    }
                }
                Map<String, Object> cell = new LinkedHashMap<>();
                cell.put("count", names.size());
                cell.put("courses", names);
                cells.add(cell);
                total += names.size();
            }
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("index", d + 1);
            day.put("total", total);
            day.put("cells", cells);
            days.add(day);
        }
        return days;
    }

    // ==================== 考试 ====================

    /** 近 {@link #EXAM_WINDOW_DAYS} 天内的考试最多 {@link #EXAM_LIMIT} 场；之后还有几场、最近的一场是什么 */
    private Map<String, Object> examSection(LocalDateTime now) {
        String from = now.format(MINUTE_FORMAT);
        String until = now.plusDays(EXAM_WINDOW_DAYS).format(MINUTE_FORMAT);
        LocalDate today = now.toLocalDate();
        List<Map<String, Object>> upcoming = dashboardMapper.examsFrom(from, EXAM_LIMIT + 1);
        List<Map<String, Object>> soon = new ArrayList<>();
        Map<String, Object> next = null;
        for (Map<String, Object> exam : upcoming) {
            Map<String, Object> item = new LinkedHashMap<>(exam);
            item.put("days", daysUntil(today, String.valueOf(exam.get("examTime"))));
            if (soon.size() < EXAM_LIMIT && String.valueOf(exam.get("examTime")).compareTo(until) < 0) {
                soon.add(item);
            } else {
                next = item;
                break;
            }
        }
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("windowDays", EXAM_WINDOW_DAYS);
        section.put("limit", EXAM_LIMIT);
        section.put("soon", soon);
        section.put("remaining", Math.max(0, dashboardMapper.countExamsFrom(from) - soon.size()));
        section.put("next", next);
        return section;
    }

    /** 距考试还有几天（按日历日算）；时间格式不对时返回 null */
    static Long daysUntil(LocalDate today, String examTime) {
        if (examTime == null || examTime.length() < 10) {
            return null;
        }
        try {
            return ChronoUnit.DAYS.between(today, LocalDate.parse(examTime.substring(0, 10)));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // ==================== 小工具 ====================

    private static List<String> sample(List<String> names) {
        return new ArrayList<>(names.subList(0, Math.min(TOP_ITEMS, names.size())));
    }

    private static LocalTime parseTime(String hhmm) {
        if (StrUtil.isBlank(hhmm)) {
            return null;
        }
        try {
            return LocalTime.parse(hhmm);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static long num(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    static Double round2(Object value) {
        return value instanceof Number ? Math.round(((Number) value).doubleValue() * 100) / 100.0 : null;
    }
}
