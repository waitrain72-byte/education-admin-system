package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.AppTime;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Apply;
import com.example.entity.Choice;
import com.example.entity.Course;
import com.example.entity.Examplan;
import com.example.mapper.ApplyMapper;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.ExamplanMapper;
import com.example.mapper.ScheduleMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 日程：周课表与月历。
 *
 * <ul>
 *   <li>课程：学生 = 选了的课，老师 = 自己开的课（已结课的不进课表）；只有「已开课」且在学期内的日子才算有课；</li>
 *   <li>事件：考试安排、作业截止（学生标出交没交）、学生自己的请假；</li>
 *   <li>请假影响的课：按日期逐天列出学生当天要上的课，请假预览和批准后补记考勤共用。</li>
 * </ul>
 */
@Service
public class ScheduleService {

    static final String ACTIVE = "已开课";
    static final String FINISHED = "已结课";

    private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private ConfigService configService;
    @Resource
    private ChoiceMapper choiceMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private ExamplanMapper examplanMapper;
    @Resource
    private ApplyMapper applyMapper;
    @Resource
    private ScheduleMapper scheduleMapper;

    /** 含 date 那一周（周一到周日）的课表与事件；date 为空或格式不对时取今天 */
    public Map<String, Object> week(String date) {
        Account me = TokenUtils.getCurrentUser();
        LocalDate today = LocalDate.now(AppTime.clock());
        LocalDate monday = parseDate(date, today).with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        Map<String, Object> semester = configService.semester();
        LocalDate[] window = semesterWindow(semester);

        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", d.toString());
            day.put("weekday", ConfigService.weekdayName(d.getDayOfWeek()));
            day.put("today", d.equals(today));
            day.put("inSemester", inWindow(d, window));
            days.add(day);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("today", today.toString());
        data.put("now", LocalDateTime.now(AppTime.clock()).format(MINUTES));
        data.put("semester", semester);
        data.put("weekNo", window == null ? null : ConfigService.currentWeek(window[0], monday));
        data.put("monday", monday.toString());
        data.put("days", days);
        data.put("courses", myCourses(me));
        data.put("events", events(me, monday, sunday));
        data.put("leaves", myLeaves(me, monday, sunday));
        return data;
    }

    /** 某月（yyyy-MM，默认本月）的月历：按整周铺满，每天列出当天的课；事件与请假同周视图 */
    public Map<String, Object> month(String month) {
        Account me = TokenUtils.getCurrentUser();
        LocalDate today = LocalDate.now(AppTime.clock());
        YearMonth ym = parseMonth(month, YearMonth.from(today));
        LocalDate gridStart = ym.atDay(1).with(DayOfWeek.MONDAY);
        LocalDate gridEnd = ym.atEndOfMonth().with(DayOfWeek.SUNDAY);
        LocalDate[] window = semesterWindow(configService.semester());
        List<Map<String, Object>> courses = myCourses(me);

        List<Map<String, Object>> days = new ArrayList<>();
        for (LocalDate d = gridStart; !d.isAfter(gridEnd); d = d.plusDays(1)) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", d.toString());
            day.put("inMonth", YearMonth.from(d).equals(ym));
            day.put("today", d.equals(today));
            day.put("classes", inWindow(d, window) ? classesOn(courses, d) : Collections.emptyList());
            days.add(day);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("month", ym.toString());
        data.put("today", today.toString());
        data.put("now", LocalDateTime.now(AppTime.clock()).format(MINUTES));
        data.put("days", days);
        data.put("events", events(me, gridStart, gridEnd));
        data.put("leaves", myLeaves(me, gridStart, gridEnd));
        return data;
    }

    /**
     * 学生在 [from, to] 里每天要上的课（只算已开课的课，学期外的日子没有课）。
     */
    public List<Map<String, Object>> classesBetween(Integer studentId, LocalDate from, LocalDate to) {
        LocalDate[] window = semesterWindow(configService.semester());
        List<Map<String, Object>> courses = studentCourses(studentId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (!inWindow(d, window)) {
                continue;
            }
            for (Map<String, Object> c : classesOn(courses, d)) {
                Map<String, Object> row = new LinkedHashMap<>(c);
                row.put("date", d.toString());
                row.put("weekday", ConfigService.weekdayName(d.getDayOfWeek()));
                rows.add(row);
            }
        }
        return rows;
    }

    /** 课表里的课程卡片：学生 = 选了的课，老师 = 自己开的课，都不含已结课；管理员没有课表 */
    List<Map<String, Object>> myCourses(Account me) {
        if (RoleEnum.STUDENT.name().equals(me.getRole())) {
            return studentCourses(me.getId());
        }
        if (RoleEnum.TEACHER.name().equals(me.getRole())) {
            Course probe = new Course();
            probe.setTeacherId(me.getId());
            List<Map<String, Object>> cards = new ArrayList<>();
            for (Course c : courseMapper.selectAll(probe)) {
                if (!FINISHED.equals(c.getStatus())) {
                    cards.add(WorkbenchService.courseCard(c.getId(), c.getName(), c.getType(), c.getScore(),
                            c.getTeacherName(), c.getRoom(), c.getWeek(), c.getSegment(), c.getStatus(), c.getNum()));
                }
            }
            return cards;
        }
        return new ArrayList<>();
    }

    private List<Map<String, Object>> studentCourses(Integer studentId) {
        Choice probe = new Choice();
        probe.setStudentId(studentId);
        List<Map<String, Object>> cards = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (Choice c : choiceMapper.selectAll(probe)) {
            if (FINISHED.equals(c.getStatus()) || !seen.add(c.getCourseId())) {
                continue;
            }
            Map<String, Object> card = WorkbenchService.courseCard(c.getCourseId(), c.getName(), c.getType(), c.getScore(),
                    c.getTeacherName(), c.getRoom(), c.getWeek(), c.getSegment(), c.getStatus(), c.getNum());
            card.put("teacherId", c.getTeacherId());
            cards.add(card);
        }
        return cards;
    }

    /** 某天要上的课：已开课、星期对得上，按节次先后 */
    static List<Map<String, Object>> classesOn(List<Map<String, Object>> courses, LocalDate date) {
        String weekday = ConfigService.weekdayName(date.getDayOfWeek());
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> c : courses) {
            if (ACTIVE.equals(c.get("status")) && weekday.equals(c.get("week"))) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("courseId", c.get("id"));
                item.put("courseName", c.get("name"));
                item.put("segment", c.get("segment"));
                item.put("start", c.get("start"));
                item.put("end", c.get("end"));
                item.put("room", c.get("room"));
                item.put("teacherName", c.get("teacherName"));
                item.put("teacherId", c.get("teacherId"));
                list.add(item);
            }
        }
        list.sort(Comparator.comparingInt(c -> WorkbenchService.segmentOrder((String) c.get("segment"))));
        return list;
    }

    /** [from, to] 里的考试与作业截止 */
    private List<Map<String, Object>> events(Account me, LocalDate from, LocalDate to) {
        List<Map<String, Object>> events = new ArrayList<>();
        String fromText = from.toString();
        String toText = to.toString();
        for (Examplan e : examplanMapper.selectAll(new Examplan())) {
            String time = e.getExamTime();
            if (StrUtil.isBlank(time) || time.length() < 10) {
                continue;
            }
            String day = time.substring(0, 10);
            if (day.compareTo(fromText) >= 0 && day.compareTo(toText) <= 0) {
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("type", "exam");
                event.put("id", e.getId());
                event.put("date", day);
                event.put("time", time.length() >= 16 ? time.substring(11, 16) : "");
                event.put("title", e.getName());
                events.add(event);
            }
        }

        List<Integer> courseIds = new ArrayList<>();
        for (Map<String, Object> c : myCourses(me)) {
            courseIds.add((Integer) c.get("id"));
        }
        if (!courseIds.isEmpty()) {
            Set<Integer> done = RoleEnum.STUDENT.name().equals(me.getRole())
                    ? new HashSet<>(scheduleMapper.submittedAssignmentIds(me.getId()))
                    : Collections.emptySet();
            String upper = to.plusDays(1).toString() + " 00:00";
            for (Map<String, Object> row : scheduleMapper.assignmentsBetween(courseIds, fromText + " 00:00", upper)) {
                String deadline = String.valueOf(row.get("deadline"));
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("type", "deadline");
                event.put("id", row.get("id"));
                event.put("date", deadline.substring(0, 10));
                event.put("time", deadline.length() >= 16 ? deadline.substring(11, 16) : "");
                event.put("title", row.get("title"));
                event.put("courseId", row.get("courseId"));
                event.put("courseName", row.get("courseName"));
                if (RoleEnum.STUDENT.name().equals(me.getRole())) {
                    event.put("done", done.contains(toInt(row.get("id"))));
                }
                events.add(event);
            }
        }
        events.sort(Comparator.comparing((Map<String, Object> e) -> String.valueOf(e.get("date")))
                .thenComparing(e -> String.valueOf(e.get("time"))));
        return events;
    }

    /** 学生自己和 [from, to] 有交集的请假（含待审核、未通过），其他角色为空 */
    private List<Map<String, Object>> myLeaves(Account me, LocalDate from, LocalDate to) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!RoleEnum.STUDENT.name().equals(me.getRole())) {
            return list;
        }
        Apply probe = new Apply();
        probe.setStudentId(me.getId());
        for (Apply a : applyMapper.selectAll(probe)) {
            LocalDate start = parseDate(a.getTime(), null);
            if (start == null) {
                continue;
            }
            int days = a.getDay() == null || a.getDay() < 1 ? 1 : a.getDay();
            LocalDate end = start.plusDays(days - 1L);
            if (end.isBefore(from) || start.isAfter(to)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("from", start.toString());
            item.put("to", end.toString());
            item.put("days", days);
            item.put("status", a.getStatus());
            item.put("content", a.getContent());
            item.put("descr", a.getDescr());
            list.add(item);
        }
        return list;
    }

    /** 学期的起止日 [开学日, 开学日 + 周数 × 7)；没设开学日返回 null（不限制） */
    static LocalDate[] semesterWindow(Map<String, Object> semester) {
        LocalDate start = parseDate(String.valueOf(semester.get("startDate")), null);
        if (start == null) {
            return null;
        }
        Object weeks = semester.get("weeks");
        int n = weeks instanceof Number ? ((Number) weeks).intValue() : 0;
        return new LocalDate[]{start, n > 0 ? start.plusWeeks(n) : null};
    }

    static boolean inWindow(LocalDate d, LocalDate[] window) {
        if (window == null) {
            return true;
        }
        return !d.isBefore(window[0]) && (window[1] == null || d.isBefore(window[1]));
    }

    static LocalDate parseDate(String text, LocalDate fallback) {
        if (text == null || text.length() < 10) {
            return fallback;
        }
        try {
            return LocalDate.parse(text.substring(0, 10));
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private static YearMonth parseMonth(String text, YearMonth fallback) {
        if (text == null || text.length() != 7) {
            return fallback;
        }
        try {
            return YearMonth.parse(text);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private static Integer toInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }
}
