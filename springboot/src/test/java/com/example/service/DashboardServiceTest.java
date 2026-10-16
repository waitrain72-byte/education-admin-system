package com.example.service;

import com.example.common.AppTime;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.exception.CustomException;
import com.example.mapper.CourseMapper;
import com.example.mapper.DashboardMapper;
import com.example.mapper.WorkbenchMapper;
import com.example.support.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 数据大屏：谁能看、看到的范围，以及「数据很多时」各块的取舍规则。
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");
    private static final String SEG1 = "第一大节（08:30 ~ 10:10）";
    private static final String SEG2 = "第二大节（10:30 ~ 12:10）";
    private static final String SEG3 = "第三大节（14:00 ~ 15:40）";

    @Mock
    private DashboardMapper dashboardMapper;
    @Mock
    private WorkbenchMapper workbenchMapper;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private ConfigService configService;
    @Mock
    private WarningService warningService;

    @InjectMocks
    private DashboardService service;

    @BeforeEach
    void setUp() {
        // 2026-10-13（星期二）10:38，第 7 教学周
        AppTime.use(Clock.fixed(Instant.parse("2026-10-13T02:38:00Z"), BEIJING));
    }

    @AfterEach
    void tearDown() {
        AppTime.use(Clock.system(BEIJING));
        CurrentUser.clear();
    }

    // ==================== 谁能看、看到什么范围 ====================

    @Test
    @DisplayName("身份：管理员看全校，教师只看本人，学生（含账号缺 id 的教师）一律拒绝")
    void viewerTeacherId() {
        assertNull(DashboardService.viewerTeacherId(account("ADMIN", 1)));
        assertEquals(7, DashboardService.viewerTeacherId(account("TEACHER", 7)));
        assertThrows(CustomException.class, () -> DashboardService.viewerTeacherId(account("STUDENT", 3)));
        assertThrows(CustomException.class, () -> DashboardService.viewerTeacherId(account("TEACHER", null)));
    }

    @Test
    @DisplayName("学生打开大屏：直接拒绝，一条统计都不查")
    void studentDenied() {
        CurrentUser.as("STUDENT", 3, "张三");
        assertThrows(CustomException.class, () -> service.screen());
        verifyNoInteractions(dashboardMapper, courseMapper, warningService);
    }

    @Test
    @DisplayName("教师：所有统计（含考试）都带上本人 id，学期内的考勤按开学日期到第 18 周末为止；管理员才有的数据不给")
    void teacherScope() {
        when(configService.semester()).thenReturn(semester());
        when(courseMapper.selectAll(any())).thenReturn(Collections.singletonList(course(5, "数据结构", "星期二", SEG2, "已开课")));
        CurrentUser.as("TEACHER", 7, "路易斯");

        Map<String, Object> data = service.screen();

        verify(dashboardMapper).attendanceByWeek("2026-08-31", "2027-01-04", 7);
        verify(dashboardMapper).countUngraded(7);
        verify(dashboardMapper).countDueSubmissions("2026-10-13 10:38", 7);
        verify(dashboardMapper).countStudentsOfTeacher(7);
        verify(dashboardMapper).examsFrom(anyString(), anyInt(), eq(7));
        verify(dashboardMapper).countExamsFrom(anyString(), eq(7));
        verify(workbenchMapper).countStudentsByTeacher(7);
        verify(workbenchMapper, never()).countStudentsAll();
        verify(dashboardMapper, never()).countStudent();
        verify(dashboardMapper, never()).countPendingApply();
        assertTrue(data.containsKey("teacher"));
        assertFalse(((Map<?, ?>) data.get("warnings")).containsKey("byCollege"));
        assertTrue(((Map<?, ?>) data.get("attendance")).containsKey("courses"));
    }

    @Test
    @DisplayName("管理员：统计不限教师；有按学院的预警分布和全校出勤排名")
    void adminScope() {
        when(configService.semester()).thenReturn(semester());
        CurrentUser.as("ADMIN", 1, "管理员");

        Map<String, Object> data = service.screen();

        verify(dashboardMapper).countUngraded(null);
        verify(dashboardMapper).examsFrom(anyString(), anyInt(), isNull());
        verify(dashboardMapper).attendanceByWeek("2026-08-31", "2027-01-04", null);
        verify(workbenchMapper).countStudentsAll();
        assertFalse(data.containsKey("teacher"));
        assertTrue(((Map<?, ?>) data.get("warnings")).containsKey("byCollege"));
        assertTrue(((Map<?, ?>) data.get("attendance")).containsKey("ranking"));
        assertTrue(((Map<?, ?>) data.get("todos")).containsKey("pendingApply"));
    }

    @Test
    @DisplayName("缓存：同一个人 30 秒内再看不重新查库，不同教师各算各的")
    void cachePerViewer() {
        when(configService.semester()).thenReturn(semester());
        CurrentUser.as("TEACHER", 7, "路易斯");
        service.screen();
        service.screen();
        CurrentUser.as("TEACHER", 8, "陈敏");
        service.screen();

        verify(courseMapper, times(2)).selectAll(any());
        verify(dashboardMapper).countUngraded(7);
        verify(dashboardMapper).countUngraded(8);
    }

    @Test
    @DisplayName("开学日期没配好：不查考勤，出勤走势为空，其余照常")
    void noSemesterStart() {
        Map<String, Object> semester = semester();
        semester.put("startDate", "");
        when(configService.semester()).thenReturn(semester);
        CurrentUser.as("ADMIN", 1, "管理员");

        Map<String, Object> data = service.screen();

        verify(dashboardMapper, never()).attendanceByWeek(any(), any(), any());
        assertEquals(Collections.emptyList(), ((Map<?, ?>) data.get("attendance")).get("weekly"));
    }

    // ==================== 各块的计算规则 ====================

    @Test
    @DisplayName("每周出勤率：按周次排好，只请了假的周不算（没有分母）")
    void weeklyRates() {
        List<Map<String, Object>> rows = Arrays.asList(
                weekRow(2, "正常", 18), weekRow(2, "缺勤", 2),
                weekRow(1, "正常", 9), weekRow(1, "迟到", 1),
                weekRow(3, "请假", 4));
        List<Map<String, Object>> weekly = DashboardService.weeklyRates(rows);
        assertEquals(2, weekly.size());
        assertEquals(1, weekly.get(0).get("week"));
        assertEquals(100.0, weekly.get(0).get("rate"));
        assertEquals(90.0, weekly.get(1).get("rate"));
        assertEquals(18L, weekly.get(1).get("present"));
        assertEquals(20L, weekly.get(1).get("counted"));
    }

    @Test
    @DisplayName("出勤排名：只列最低的 5 门，人少于 5 的课不排名，开课却没记录的单独计数，已结课的没记录不算")
    void courseRanking() {
        List<Course> courses = new ArrayList<>();
        Map<Integer, Long> enrolled = new HashMap<>();
        Map<Integer, long[]> perCourse = new HashMap<>();
        // 6 门可排名的课：出勤率 80、85、91、93、96、99
        double[] rates = {96, 80, 99, 91, 85, 93};
        for (int i = 0; i < rates.length; i++) {
            int id = i + 1;
            courses.add(course(id, "课" + id, "星期一", SEG1, "已开课"));
            enrolled.put(id, 20L);
            perCourse.put(id, new long[]{(long) rates[i], 100});
        }
        courses.add(course(11, "小班课", "星期一", SEG1, "已开课"));
        enrolled.put(11, 2L);
        perCourse.put(11, new long[]{1, 2});
        courses.add(course(12, "没点过名", "星期二", SEG1, "已开课"));
        courses.add(course(13, "上学期的课", "星期二", SEG1, "已结课"));

        Map<String, Object> ranking = DashboardService.courseRanking(courses, enrolled, perCourse);

        List<?> lowest = (List<?>) ranking.get("lowest");
        assertEquals(5, lowest.size());
        assertEquals(Arrays.asList(80.0, 85.0, 91.0, 93.0, 96.0),
                lowest.stream().map(m -> ((Map<?, ?>) m).get("rate")).collect(Collectors.toList()));
        Map<?, ?> bands = (Map<?, ?>) ranking.get("bands");
        assertEquals(2, bands.get("good"));
        assertEquals(2, bands.get("fair"));
        assertEquals(2, bands.get("low"));
        assertEquals(1, ranking.get("smallCount"));
        assertEquals(Collections.singletonList("小班课"), ranking.get("smallNames"));
        assertEquals(1, ranking.get("noRecordCount"));
        assertEquals(Collections.singletonList("没点过名"), ranking.get("noRecordNames"));
    }

    @Test
    @DisplayName("当前作业：优先还没截止的里最早的一份，都截止了取最近截止的；没有作业为 null")
    void currentAssignment() {
        String now = "2026-10-13 10:38";
        List<Map<String, Object>> list = Arrays.asList(
                assignment("实验一", "2026-09-27 23:59"), assignment("实验二", "2026-10-12 23:59"),
                assignment("实验四", "2026-10-30 23:59"), assignment("实验三", "2026-10-15 23:59"));
        assertEquals("实验三", DashboardService.currentAssignment(list, now).get("title"));
        assertEquals("实验二", DashboardService.currentAssignment(list.subList(0, 2), now).get("title"));
        assertNull(DashboardService.currentAssignment(Collections.emptyList(), now));
    }

    @Test
    @DisplayName("教师的各门课：只列正在开课的，带出勤率和当前作业是否已截止；没有考勤的出勤率为 null")
    void teacherCourses() {
        List<Course> courses = Arrays.asList(
                course(1, "数据结构", "星期二", SEG2, "已开课"),
                course(2, "线性代数", "星期五", SEG3, "已开课"),
                course(3, "C 语言程序设计", "星期三", SEG2, "已结课"));
        Map<Integer, Long> enrolled = new HashMap<>();
        enrolled.put(1, 15L);
        enrolled.put(2, 2L);
        Map<Integer, long[]> perCourse = new HashMap<>();
        perCourse.put(1, new long[]{70, 75});
        Map<String, Object> hw = assignment("作业三", "2026-10-15 23:59");
        hw.put("courseId", 1);
        hw.put("submitted", 5L);

        List<Map<String, Object>> rows = DashboardService.teacherCourses(courses, enrolled, perCourse,
                Collections.singletonList(hw), LocalDateTime.of(2026, 10, 13, 10, 38));

        assertEquals(2, rows.size());
        assertEquals(93.3, rows.get(0).get("rate"));
        Map<?, ?> current = (Map<?, ?>) rows.get(0).get("assignment");
        assertEquals(5L, current.get("submitted"));
        assertEquals(15L, current.get("students"));
        assertEquals(false, current.get("closed"));
        assertNull(rows.get(1).get("rate"));
        assertNull(rows.get(1).get("assignment"));
    }

    @Test
    @DisplayName("节次状态：没到 / 正在上（含起止时刻）/ 已过；时间解析不了按还没到")
    void phase() {
        assertEquals("upcoming", DashboardService.phase(LocalTime.of(10, 29), "10:30", "12:10"));
        assertEquals("ongoing", DashboardService.phase(LocalTime.of(10, 30), "10:30", "12:10"));
        assertEquals("ongoing", DashboardService.phase(LocalTime.of(12, 10), "10:30", "12:10"));
        assertEquals("finished", DashboardService.phase(LocalTime.of(12, 11), "10:30", "12:10"));
        assertEquals("upcoming", DashboardService.phase(LocalTime.of(12, 11), "", ""));
        assertEquals(8, DashboardService.minutesSince(LocalTime.of(10, 38), "10:30"));
    }

    @Test
    @DisplayName("本周排课：七天 × 五个大节，只算正在开课的课")
    void weekSection() {
        List<Course> courses = Arrays.asList(
                course(1, "高等数学", "星期一", SEG1, "已开课"),
                course(2, "大学英语", "星期一", SEG1, "已开课"),
                course(3, "数据结构", "星期二", SEG2, "已开课"),
                course(4, "大学物理", "星期二", SEG2, "已结课"));
        List<Map<String, Object>> days = DashboardService.weekSection(courses, Collections.emptyMap());
        assertEquals(7, days.size());
        assertEquals(2, days.get(0).get("total"));
        assertEquals(2, ((Map<?, ?>) ((List<?>) days.get(0).get("cells")).get(0)).get("count"));
        assertEquals(1, ((Map<?, ?>) ((List<?>) days.get(1).get("cells")).get(1)).get("count"));
        assertEquals(0, days.get(6).get("total"));
    }

    @Test
    @DisplayName("按学院的预警：预警多的学院在前，人数相同按名称，最多 5 个，带学院总人数")
    void collegeBreakdown() {
        List<Map<String, Object>> warned = new ArrayList<>();
        Map<Integer, Map<String, Object>> orgs = new HashMap<>();
        String[] colleges = {"软件学院", "软件学院", "信息学院", "材料学院", "马克思主义学院", "外语学院", "数学学院"};
        for (int i = 0; i < colleges.length; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("studentId", i + 1);
            warned.add(row);
            Map<String, Object> org = new HashMap<>();
            org.put("college", colleges[i]);
            orgs.put(i + 1, org);
        }
        Map<String, Object> total = new HashMap<>();
        total.put("name", "软件学院");
        total.put("total", 300L);

        List<Map<String, Object>> rows = DashboardService.collegeBreakdown(warned, orgs, Collections.singletonList(total));

        assertEquals(5, rows.size());
        assertEquals("软件学院", rows.get(0).get("name"));
        assertEquals(2L, rows.get(0).get("warned"));
        assertEquals(300L, rows.get(0).get("total"));
        assertEquals("信息学院", rows.get(1).get("name"));
    }

    @Test
    @DisplayName("距考试天数：按日历日算，格式不对返回 null")
    void daysUntil() {
        LocalDate today = LocalDate.of(2026, 10, 13);
        assertEquals(8L, DashboardService.daysUntil(today, "2026-10-21 14:00"));
        assertEquals(0L, DashboardService.daysUntil(today, "2026-10-13 23:00"));
        assertNull(DashboardService.daysUntil(today, "下周"));
        assertNull(DashboardService.daysUntil(today, null));
    }

    // ==================== 构造测试数据 ====================

    private static Account account(String role, Integer id) {
        Account account = new Account();
        account.setRole(role);
        account.setId(id);
        return account;
    }

    private static Map<String, Object> semester() {
        Map<String, Object> semester = new LinkedHashMap<>();
        semester.put("name", "2026-2027 学年第一学期");
        semester.put("startDate", "2026-08-31");
        semester.put("weeks", 18);
        semester.put("currentWeek", 7);
        semester.put("today", "2026-10-13");
        semester.put("weekday", "星期二");
        return semester;
    }

    private static Course course(int id, String name, String week, String segment, String status) {
        Course c = new Course();
        c.setId(id);
        c.setName(name);
        c.setWeek(week);
        c.setSegment(segment);
        c.setStatus(status);
        c.setRoom("77" + id);
        c.setTeacherName("路易斯");
        return c;
    }

    private static Map<String, Object> weekRow(int week, String status, long count) {
        Map<String, Object> row = new HashMap<>();
        row.put("week", (long) week);
        row.put("name", status);
        row.put("value", count);
        return row;
    }

    private static Map<String, Object> assignment(String title, String deadline) {
        Map<String, Object> row = new HashMap<>();
        row.put("title", title);
        row.put("deadline", deadline);
        return row;
    }
}
