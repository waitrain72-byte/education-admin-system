package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.AppTime;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Apply;
import com.example.entity.Attendance;
import com.example.entity.AttendanceSession;
import com.example.entity.Course;
import com.example.entity.Student;
import com.example.exception.CustomException;
import com.example.mapper.ApplyMapper;
import com.example.mapper.AttendanceMapper;
import com.example.mapper.AttendanceSessionMapper;
import com.example.mapper.CourseSpaceMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 课堂签到：老师发起一场签到（4 位签到码，默认 5 分钟），学生在截止前输入签到码完成签到。
 *
 * <ul>
 *   <li>签到写进考勤表（time = 上课日期，status = 正常），同一学生同一门课同一天只有一条考勤；</li>
 *   <li>签到结束（老师手动结束，或过了截止时间由定时任务收尾）时，没签到的学生记缺勤——
 *       当天有审核通过的请假的记请假；老师之后还可以逐个改状态；</li>
 *   <li>同一场签到每个学生最多输错 5 次，防止把 4 位码挨个试出来。</li>
 * </ul>
 */
@Service
public class AttendanceSessionService {

    public static final String NORMAL = "正常";
    public static final String ABSENT = "缺勤";
    public static final String LEAVE = "请假";
    /** 老师可以手工登记的考勤状态 */
    public static final List<String> STATUSES = Arrays.asList("正常", "迟到", "早退", "缺勤", "请假");

    static final int MAX_WRONG_ATTEMPTS = 5;
    static final int DEFAULT_MINUTES = 5;
    static final int MAX_MINUTES = 30;
    /** 历次签到汇总最多返回的天数 */
    private static final int HISTORY_DAYS = 30;

    private static final DateTimeFormatter SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 签到码输错次数：key 为「场次 ID:学生 ID」，场次结束时清掉 */
    private final Map<String, AtomicInteger> wrongAttempts = new ConcurrentHashMap<>();

    @Resource
    private CourseSpaceService courseSpaceService;
    @Resource
    private AttendanceSessionMapper sessionMapper;
    @Resource
    private AttendanceMapper attendanceMapper;
    @Resource
    private CourseSpaceMapper courseSpaceMapper;
    @Resource
    private ApplyMapper applyMapper;

    /**
     * 签到页数据。
     *
     * <p>老师（管理员）：进行中的签到（含签到码、已签人数）、某一天的名单状态、历次签到汇总；
     * 学生：是否正在签到（不含签到码）、本人是否已签、本人的考勤记录与统计。</p>
     *
     * @param date 老师看哪一天的名单，默认今天
     */
    public Map<String, Object> panel(Integer courseId, String date) {
        Course course = courseSpaceService.requireCourse(courseId);
        String relation = courseSpaceService.requireMember(course);
        AttendanceSession active = activeSession(courseId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("now", now());

        if (CourseSpaceService.REL_STUDENT.equals(relation)) {
            Integer studentId = TokenUtils.getCurrentUser().getId();
            List<Attendance> records = courseSpaceMapper.selectAttendanceOfStudent(courseId, studentId);
            if (active != null) {
                Map<String, Object> brief = new LinkedHashMap<>();
                brief.put("id", active.getId());
                brief.put("expireTime", active.getExpireTime());
                brief.put("signed", records.stream().anyMatch(r -> active.getId().equals(r.getSessionId())
                        && NORMAL.equals(r.getStatus())));
                brief.put("locked", attemptsOf(active.getId(), studentId) >= MAX_WRONG_ATTEMPTS);
                data.put("active", brief);
            } else {
                data.put("active", null);
            }
            data.put("records", records);
            data.put("counts", countByStatus(records));
            return data;
        }

        String day = isDate(date) ? date : (active != null ? active.getDate() : AppTime.today());
        data.put("active", active == null ? null : sessionView(active));
        data.put("date", day);
        data.put("roster", roster(courseId, day));
        data.put("history", history(courseId));
        return data;
    }

    /** 发起签到：已有进行中的就直接返回那一场（重复点按钮不会开出两场） */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> start(Integer courseId, Integer minutes) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireTeaching(course);

        AttendanceSession current = sessionMapper.selectActiveByCourse(courseId);
        if (current != null) {
            if (!isExpired(current)) {
                return sessionView(current);
            }
            // 过了截止时间但定时任务还没来得及收尾：先结掉，再开新的
            finishSession(current, current.getExpireTime());
        }

        int span = minutes == null ? DEFAULT_MINUTES : Math.max(1, Math.min(minutes, MAX_MINUTES));
        LocalDateTime now = LocalDateTime.now(AppTime.clock());
        AttendanceSession session = new AttendanceSession();
        session.setCourseId(courseId);
        session.setTeacherId(course.getTeacherId());
        session.setCode(String.format("%04d", RANDOM.nextInt(10000)));
        session.setDate(now.toLocalDate().toString());
        session.setStartTime(now.format(SECONDS));
        session.setExpireTime(now.plusMinutes(span).format(SECONDS));
        session.setStatus(AttendanceSession.STATUS_ACTIVE);
        sessionMapper.insert(session);
        return sessionView(session);
    }

    /** 老师提前结束签到：没签的记缺勤（有请假的记请假） */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> finish(Integer courseId, Integer sessionId) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireTeaching(course);
        AttendanceSession session = sessionMapper.selectById(sessionId);
        if (session == null || !courseId.equals(session.getCourseId())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", session.getDate());
        if (!AttendanceSession.STATUS_ACTIVE.equals(session.getStatus())) {
            result.put("marked", 0);
            return result;
        }
        // 已经过了截止时间的，结束时间记原定的截止时间；否则记现在
        String end = isExpired(session) ? session.getExpireTime() : now();
        result.put("marked", finishSession(session, end));
        return result;
    }

    /** 定时任务调用：结束一场已过截止时间的签到 */
    @Transactional(rollbackFor = Exception.class)
    public int finishExpired(AttendanceSession session) {
        return finishSession(session, session.getExpireTime());
    }

    /** 已过截止时间、还标着进行中的场次 */
    public List<AttendanceSession> expiredSessions() {
        return sessionMapper.selectExpired(now());
    }

    /** 学生签到 */
    @Transactional(rollbackFor = Exception.class)
    public Attendance checkin(Integer courseId, String code) {
        Course course = courseSpaceService.requireCourse(courseId);
        if (!CourseSpaceService.REL_STUDENT.equals(courseSpaceService.relationOf(course))) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        Integer studentId = TokenUtils.getCurrentUser().getId();
        AttendanceSession session = activeSession(courseId);
        if (session == null) {
            throw new CustomException(ResultCodeEnum.SIGN_NOT_OPEN_ERROR);
        }
        String key = session.getId() + ":" + studentId;
        if (attemptsOf(session.getId(), studentId) >= MAX_WRONG_ATTEMPTS) {
            throw new CustomException(ResultCodeEnum.SIGN_LOCKED_ERROR);
        }
        if (code == null || !session.getCode().equals(code.trim())) {
            int wrong = wrongAttempts.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
            throw new CustomException(wrong >= MAX_WRONG_ATTEMPTS
                    ? ResultCodeEnum.SIGN_LOCKED_ERROR : ResultCodeEnum.SIGN_CODE_ERROR);
        }

        Attendance existing = attendanceMapper.selectByStudentIdAndCourseIdAndTime(studentId, courseId, session.getDate());
        if (existing != null) {
            if (NORMAL.equals(existing.getStatus()) && session.getId().equals(existing.getSessionId())) {
                return existing;
            }
            existing.setStatus(NORMAL);
            existing.setSessionId(session.getId());
            attendanceMapper.updateById(existing);
            return existing;
        }
        Attendance record = new Attendance();
        record.setStudentId(studentId);
        record.setCourseId(courseId);
        record.setTeacherId(session.getTeacherId());
        record.setTime(session.getDate());
        record.setStatus(NORMAL);
        record.setSessionId(session.getId());
        attendanceMapper.insert(record);
        return record;
    }

    /** 老师手工登记或修改某个学生某一天的考勤 */
    @Transactional(rollbackFor = Exception.class)
    public Attendance mark(Integer courseId, Integer studentId, String date, String status) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireTeaching(course);
        if (studentId == null || !isDate(date) || !STATUSES.contains(status)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        if (!courseSpaceMapper.selectMemberIds(courseId).contains(studentId)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Attendance existing = attendanceMapper.selectByStudentIdAndCourseIdAndTime(studentId, courseId, date);
        if (existing != null) {
            existing.setStatus(status);
            attendanceMapper.updateById(existing);
            return existing;
        }
        Attendance record = new Attendance();
        record.setStudentId(studentId);
        record.setCourseId(courseId);
        record.setTeacherId(course.getTeacherId());
        record.setTime(date);
        record.setStatus(status);
        attendanceMapper.insert(record);
        return record;
    }

    /** 课程里正在签到（没过截止时间）的场次，没有返回 null */
    public AttendanceSession activeSession(Integer courseId) {
        AttendanceSession session = sessionMapper.selectActiveByCourse(courseId);
        return session == null || isExpired(session) ? null : session;
    }

    /**
     * 结束场次并给没签到的学生补记考勤。并发安全：只有把状态从「进行中」改掉的那一次请求负责补记。
     *
     * @return 补记的人数
     */
    private int finishSession(AttendanceSession session, String endTime) {
        if (sessionMapper.finish(session.getId(), endTime) == 0) {
            return 0;
        }
        clearAttempts(session.getId());

        Set<Integer> recorded = new HashSet<>();
        for (Attendance a : courseSpaceMapper.selectAttendanceOfDate(session.getCourseId(), session.getDate())) {
            recorded.add(a.getStudentId());
        }
        List<Apply> leaves = approvedLeaves();
        int marked = 0;
        for (Integer studentId : courseSpaceMapper.selectMemberIds(session.getCourseId())) {
            if (recorded.contains(studentId)) {
                continue;
            }
            Attendance record = new Attendance();
            record.setStudentId(studentId);
            record.setCourseId(session.getCourseId());
            record.setTeacherId(session.getTeacherId());
            record.setTime(session.getDate());
            record.setStatus(onLeave(leaves, studentId, session.getDate()) ? LEAVE : ABSENT);
            record.setSessionId(session.getId());
            attendanceMapper.insert(record);
            marked++;
        }
        return marked;
    }

    private List<Apply> approvedLeaves() {
        Apply probe = new Apply();
        probe.setStatus("审核通过");
        return applyMapper.selectAll(probe);
    }

    /** 请假从 time 当天起共 day 天，审核通过的才算 */
    static boolean onLeave(List<Apply> approved, Integer studentId, String date) {
        if (studentId == null || !isDate(date)) {
            return false;
        }
        LocalDate day = LocalDate.parse(date);
        for (Apply apply : approved) {
            if (!studentId.equals(apply.getStudentId()) || StrUtil.isBlank(apply.getTime())) {
                continue;
            }
            try {
                String start = apply.getTime().trim();
                LocalDate from = LocalDate.parse(start.substring(0, Math.min(10, start.length())));
                int days = apply.getDay() == null || apply.getDay() < 1 ? 1 : apply.getDay();
                if (!day.isBefore(from) && day.isBefore(from.plusDays(days))) {
                    return true;
                }
            } catch (DateTimeParseException e) {
                // 格式不对的请假记录跳过，不影响其他人
            }
        }
        return false;
    }

    /** 某一天的名单：每个选课学生当天的考勤状态（没有记录为空） */
    private List<Map<String, Object>> roster(Integer courseId, String date) {
        Map<Integer, Attendance> byStudent = new HashMap<>();
        for (Attendance a : courseSpaceMapper.selectAttendanceOfDate(courseId, date)) {
            byStudent.put(a.getStudentId(), a);
        }
        List<Apply> leaves = approvedLeaves();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Student s : courseSpaceMapper.selectMembers(courseId)) {
            Attendance a = byStudent.get(s.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", s.getId());
            row.put("username", s.getUsername());
            row.put("name", s.getName());
            row.put("avatar", s.getAvatar());
            row.put("className", s.getClassName());
            row.put("status", a == null ? null : a.getStatus());
            row.put("bySession", a != null && a.getSessionId() != null && NORMAL.equals(a.getStatus()));
            // 当天有已批准的请假：名单上提示老师，签到结束时也会记成请假
            row.put("onLeave", onLeave(leaves, s.getId(), date));
            rows.add(row);
        }
        return rows;
    }

    /** 历次考勤按日期汇总（最近的在前）：date + 各状态人数 */
    private List<Map<String, Object>> history(Integer courseId) {
        Map<String, Map<String, Object>> byDate = new LinkedHashMap<>();
        for (Map<String, Object> row : courseSpaceMapper.attendanceCountsByDate(courseId)) {
            String date = String.valueOf(row.get("date"));
            Map<String, Object> item = byDate.get(date);
            if (item == null) {
                if (byDate.size() >= HISTORY_DAYS) {
                    continue;
                }
                item = new LinkedHashMap<>();
                item.put("date", date);
                for (String status : STATUSES) {
                    item.put(status, 0L);
                }
                byDate.put(date, item);
            }
            Object total = row.get("total");
            item.merge(String.valueOf(row.get("status")), total instanceof Number ? ((Number) total).longValue() : 0L,
                    (a, b) -> ((Number) a).longValue() + ((Number) b).longValue());
        }
        return new ArrayList<>(byDate.values());
    }

    private Map<String, Object> sessionView(AttendanceSession session) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", session.getId());
        view.put("code", session.getCode());
        view.put("date", session.getDate());
        view.put("startTime", session.getStartTime());
        view.put("expireTime", session.getExpireTime());
        long signed = courseSpaceMapper.selectAttendanceOfDate(session.getCourseId(), session.getDate()).stream()
                .filter(a -> session.getId().equals(a.getSessionId()) && NORMAL.equals(a.getStatus()))
                .count();
        view.put("signed", signed);
        view.put("total", courseSpaceMapper.selectMemberIds(session.getCourseId()).size());
        return view;
    }

    private static Map<String, Long> countByStatus(List<Attendance> records) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : STATUSES) {
            counts.put(status, 0L);
        }
        for (Attendance a : records) {
            if (a.getStatus() != null) {
                counts.merge(a.getStatus(), 1L, Long::sum);
            }
        }
        return counts;
    }

    private int attemptsOf(Integer sessionId, Integer studentId) {
        AtomicInteger count = wrongAttempts.get(sessionId + ":" + studentId);
        return count == null ? 0 : count.get();
    }

    private void clearAttempts(Integer sessionId) {
        String prefix = sessionId + ":";
        wrongAttempts.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private static boolean isExpired(AttendanceSession session) {
        return session.getExpireTime() == null || session.getExpireTime().compareTo(now()) < 0;
    }

    private static String now() {
        return LocalDateTime.now(AppTime.clock()).format(SECONDS);
    }

    private static boolean isDate(String value) {
        if (value == null || value.length() != 10) {
            return false;
        }
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
