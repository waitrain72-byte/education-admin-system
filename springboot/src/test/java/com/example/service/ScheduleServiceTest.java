package com.example.service;

import com.example.common.AppTime;
import com.example.entity.Apply;
import com.example.entity.Choice;
import com.example.entity.Examplan;
import com.example.mapper.ApplyMapper;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.ExamplanMapper;
import com.example.mapper.ScheduleMapper;
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
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 日程：一周的日期与课表、考试与作业截止、请假，以及「某段日期里要上的课」。
 */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    @Mock
    private ConfigService configService;
    @Mock
    private ChoiceMapper choiceMapper;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private ExamplanMapper examplanMapper;
    @Mock
    private ApplyMapper applyMapper;
    @Mock
    private ScheduleMapper scheduleMapper;

    @InjectMocks
    private ScheduleService service;

    @BeforeEach
    void setUp() {
        // 今天 = 2026-10-08（星期四），学期从 2026-08-31（星期一）开始，共 20 周
        AppTime.use(Clock.fixed(Instant.parse("2026-10-08T02:00:00Z"), BEIJING));
        Map<String, Object> semester = new LinkedHashMap<>();
        semester.put("startDate", "2026-08-31");
        semester.put("weeks", 20);
        lenient().when(configService.semester()).thenReturn(semester);
        lenient().when(choiceMapper.selectAll(any())).thenReturn(Arrays.asList(
                choice(1, "高等数学", "星期一", "第一大节（08:30 ~ 10:10）", "已开课"),
                choice(11, "体育（篮球）", "星期四", "第四大节（16:00 ~ 17:40）", "已开课"),
                choice(9, "离散数学", "星期四", "第一大节（08:30 ~ 10:10）", "已开课"),
                choice(12, "Python 数据分析", "星期四", "第五大节（19:00 ~ 20:40）", "未开课"),
                choice(14, "C 语言程序设计", "星期三", "第二大节（10:30 ~ 12:10）", "已结课")));
    }

    @AfterEach
    void tearDown() {
        AppTime.use(Clock.system(BEIJING));
        CurrentUser.clear();
    }

    @Test
    @DisplayName("某天的课：只算已开课、星期对得上的，按节次先后")
    void classesOnThursday() {
        CurrentUser.as("STUDENT", 1, "张三");

        List<Map<String, Object>> rows = service.classesBetween(1, LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 8));

        assertEquals(2, rows.size());
        assertEquals("离散数学", rows.get(0).get("courseName"));
        assertEquals("体育（篮球）", rows.get(1).get("courseName"));
        assertEquals("星期四", rows.get(0).get("weekday"));
    }

    @Test
    @DisplayName("学期外的日子没有课")
    void noClassesOutsideSemester() {
        CurrentUser.as("STUDENT", 1, "张三");

        assertTrue(service.classesBetween(1, LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 30)).isEmpty());
        // 第 21 周（2027-01-18 起）已经过了 20 周；第 20 周的星期一（2027-01-11）还有高等数学
        assertTrue(service.classesBetween(1, LocalDate.of(2027, 1, 18), LocalDate.of(2027, 1, 24)).isEmpty());
        assertEquals(1, service.classesBetween(1, LocalDate.of(2027, 1, 11), LocalDate.of(2027, 1, 11)).size());
    }

    @Test
    @DisplayName("周视图：周一到周日、标出今天、第几周；课表不含已结课的课")
    @SuppressWarnings("unchecked")
    void weekView() {
        CurrentUser.as("STUDENT", 1, "张三");
        when(examplanMapper.selectAll(any())).thenReturn(Arrays.asList(
                exam(5, "数据结构 期中测验", "2026-10-09 14:00"),
                exam(6, "大学英语四级考试", "2026-12-12 09:00")));
        Map<String, Object> deadline = new HashMap<>();
        deadline.put("id", 3);
        deadline.put("courseId", 7);
        deadline.put("courseName", "数据结构");
        deadline.put("title", "作业三：二叉树的遍历");
        deadline.put("deadline", "2026-10-11 23:59");
        when(scheduleMapper.assignmentsBetween(anyList(), eq("2026-10-05 00:00"), eq("2026-10-12 00:00")))
                .thenReturn(Collections.singletonList(deadline));
        when(scheduleMapper.submittedAssignmentIds(1)).thenReturn(Collections.singletonList(3));
        when(applyMapper.selectAll(any())).thenReturn(Collections.singletonList(leave("2026-10-09", 2, "待审核")));

        Map<String, Object> data = service.week(null);

        List<Map<String, Object>> days = (List<Map<String, Object>>) data.get("days");
        assertEquals(7, days.size());
        assertEquals("2026-10-05", days.get(0).get("date"));
        assertEquals("星期一", days.get(0).get("weekday"));
        assertTrue((Boolean) days.get(3).get("today"));
        assertEquals(6, data.get("weekNo"));

        List<Map<String, Object>> courses = (List<Map<String, Object>>) data.get("courses");
        assertEquals(4, courses.size());
        assertTrue(courses.stream().noneMatch(c -> "已结课".equals(c.get("status"))));

        List<Map<String, Object>> events = (List<Map<String, Object>>) data.get("events");
        assertEquals(2, events.size());
        assertEquals("exam", events.get(0).get("type"));
        assertEquals("14:00", events.get(0).get("time"));
        assertEquals("deadline", events.get(1).get("type"));
        assertEquals(true, events.get(1).get("done"));

        List<Map<String, Object>> leaves = (List<Map<String, Object>>) data.get("leaves");
        assertEquals("2026-10-10", leaves.get(0).get("to"));
    }

    @Test
    @DisplayName("月历：按整周铺满，月外的日子标出来，学期内已开课的日子列出当天的课")
    @SuppressWarnings("unchecked")
    void monthView() {
        CurrentUser.as("STUDENT", 1, "张三");
        when(examplanMapper.selectAll(any())).thenReturn(Collections.emptyList());
        when(scheduleMapper.assignmentsBetween(anyList(), any(), any())).thenReturn(Collections.emptyList());
        when(scheduleMapper.submittedAssignmentIds(1)).thenReturn(Collections.emptyList());
        when(applyMapper.selectAll(any())).thenReturn(Collections.emptyList());

        Map<String, Object> data = service.month("2026-10");

        List<Map<String, Object>> days = (List<Map<String, Object>>) data.get("days");
        // 2026-10-01 是星期四：从 09-28（星期一）铺到 11-01（星期日），共 5 周
        assertEquals(35, days.size());
        assertEquals("2026-09-28", days.get(0).get("date"));
        assertFalse((Boolean) days.get(0).get("inMonth"));
        Map<String, Object> oct8 = days.stream().filter(d -> "2026-10-08".equals(d.get("date"))).findFirst().orElseThrow(IllegalStateException::new);
        assertTrue((Boolean) oct8.get("today"));
        assertEquals(2, ((List<?>) oct8.get("classes")).size());
    }

    @Test
    @DisplayName("学期窗口：开学日当天算、满周数后不算；没设开学日不限制")
    void semesterWindow() {
        Map<String, Object> semester = new HashMap<>();
        semester.put("startDate", "2026-08-31");
        semester.put("weeks", 1);
        LocalDate[] window = ScheduleService.semesterWindow(semester);
        assertTrue(ScheduleService.inWindow(LocalDate.of(2026, 8, 31), window));
        assertTrue(ScheduleService.inWindow(LocalDate.of(2026, 9, 6), window));
        assertFalse(ScheduleService.inWindow(LocalDate.of(2026, 9, 7), window));
        assertFalse(ScheduleService.inWindow(LocalDate.of(2026, 8, 30), window));

        semester.put("startDate", "");
        assertTrue(ScheduleService.inWindow(LocalDate.of(2000, 1, 1), ScheduleService.semesterWindow(semester)));
    }

    private static Choice choice(int courseId, String name, String week, String segment, String status) {
        Choice c = new Choice();
        c.setCourseId(courseId);
        c.setName(name);
        c.setWeek(week);
        c.setSegment(segment);
        c.setStatus(status);
        c.setTeacherId(2);
        return c;
    }

    private static Examplan exam(int id, String name, String time) {
        Examplan e = new Examplan();
        e.setId(id);
        e.setName(name);
        e.setExamTime(time);
        return e;
    }

    private static Apply leave(String from, int days, String status) {
        Apply a = new Apply();
        a.setId(9);
        a.setStudentId(1);
        a.setTime(from);
        a.setDay(days);
        a.setStatus(status);
        return a;
    }
}
