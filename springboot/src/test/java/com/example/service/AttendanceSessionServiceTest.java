package com.example.service;

import com.example.common.AppTime;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Apply;
import com.example.entity.Attendance;
import com.example.entity.AttendanceSession;
import com.example.entity.Course;
import com.example.exception.CustomException;
import com.example.mapper.ApplyMapper;
import com.example.mapper.AttendanceMapper;
import com.example.mapper.AttendanceSessionMapper;
import com.example.mapper.CourseSpaceMapper;
import com.example.support.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 课堂签到：签到码校验与输错上限、签到写考勤、结束时补记缺勤 / 请假、重复发起。
 */
@ExtendWith(MockitoExtension.class)
class AttendanceSessionServiceTest {

    private static final int COURSE_ID = 10;
    private static final int STUDENT_ID = 1;
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    @Mock
    private CourseSpaceService courseSpaceService;
    @Mock
    private AttendanceSessionMapper sessionMapper;
    @Mock
    private AttendanceMapper attendanceMapper;
    @Mock
    private CourseSpaceMapper courseSpaceMapper;
    @Mock
    private ApplyMapper applyMapper;

    @InjectMocks
    private AttendanceSessionService service;

    private Course course;

    @BeforeEach
    void setUp() {
        // 2026-10-07 10:00:00（北京时间）
        AppTime.use(Clock.fixed(Instant.parse("2026-10-07T02:00:00Z"), BEIJING));
        course = new Course();
        course.setId(COURSE_ID);
        course.setTeacherId(5);
        lenient().when(courseSpaceService.requireCourse(COURSE_ID)).thenReturn(course);
    }

    @AfterEach
    void tearDown() {
        AppTime.use(Clock.system(BEIJING));
        CurrentUser.clear();
    }

    private void asStudent() {
        CurrentUser.as("STUDENT", STUDENT_ID, "张三");
        when(courseSpaceService.relationOf(course)).thenReturn(CourseSpaceService.REL_STUDENT);
    }

    @Test
    @DisplayName("签到码正确：写一条「正常」考勤，时间是上课日期，挂上场次")
    void correctCodeWritesAttendance() {
        asStudent();
        when(sessionMapper.selectActiveByCourse(COURSE_ID)).thenReturn(session(7, "2026-10-07 10:05:00"));

        service.checkin(COURSE_ID, " 0427 ");

        ArgumentCaptor<Attendance> saved = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceMapper).insert(saved.capture());
        assertEquals("正常", saved.getValue().getStatus());
        assertEquals("2026-10-07", saved.getValue().getTime());
        assertEquals(7, saved.getValue().getSessionId());
        assertEquals(5, saved.getValue().getTeacherId());
    }

    @Test
    @DisplayName("当天已有考勤（比如上一场记了缺勤）：改成正常，不重复插入")
    void checkinUpdatesExistingRecord() {
        asStudent();
        when(sessionMapper.selectActiveByCourse(COURSE_ID)).thenReturn(session(7, "2026-10-07 10:05:00"));
        Attendance absent = new Attendance();
        absent.setId(3);
        absent.setStatus("缺勤");
        when(attendanceMapper.selectByStudentIdAndCourseIdAndTime(STUDENT_ID, COURSE_ID, "2026-10-07")).thenReturn(absent);

        service.checkin(COURSE_ID, "0427");

        assertEquals("正常", absent.getStatus());
        assertEquals(7, absent.getSessionId());
        verify(attendanceMapper).updateById(absent);
        verify(attendanceMapper, never()).insert(any());
    }

    @Test
    @DisplayName("签到码输错 5 次后锁定，之后输对也不行")
    void wrongCodeLocksAfterFiveAttempts() {
        asStudent();
        when(sessionMapper.selectActiveByCourse(COURSE_ID)).thenReturn(session(7, "2026-10-07 10:05:00"));

        for (int i = 1; i <= 4; i++) {
            assertEquals(ResultCodeEnum.SIGN_CODE_ERROR.code, codeOf(() -> service.checkin(COURSE_ID, "1111")));
        }
        assertEquals(ResultCodeEnum.SIGN_LOCKED_ERROR.code, codeOf(() -> service.checkin(COURSE_ID, "1111")));
        assertEquals(ResultCodeEnum.SIGN_LOCKED_ERROR.code, codeOf(() -> service.checkin(COURSE_ID, "0427")));
        verify(attendanceMapper, never()).insert(any());
    }

    @Test
    @DisplayName("签到已过截止时间：提示没有进行中的签到")
    void expiredSessionIsClosed() {
        asStudent();
        when(sessionMapper.selectActiveByCourse(COURSE_ID)).thenReturn(session(7, "2026-10-07 09:59:59"));

        assertEquals(ResultCodeEnum.SIGN_NOT_OPEN_ERROR.code, codeOf(() -> service.checkin(COURSE_ID, "0427")));
    }

    @Test
    @DisplayName("没选这门课的人不能签到")
    void outsiderCannotCheckIn() {
        when(courseSpaceService.relationOf(course)).thenReturn(CourseSpaceService.REL_VISITOR);

        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, codeOf(() -> service.checkin(COURSE_ID, "0427")));
    }

    @Test
    @DisplayName("结束签到：已签的不动，有请假的记请假，其余记缺勤")
    void finishMarksAbsenteesAndLeaves() {
        AttendanceSession active = session(7, "2026-10-07 10:05:00");
        when(sessionMapper.selectById(7)).thenReturn(active);
        when(sessionMapper.finish(eq(7), anyString())).thenReturn(1);
        Attendance signed = new Attendance();
        signed.setStudentId(1);
        signed.setStatus("正常");
        when(courseSpaceMapper.selectAttendanceOfDate(COURSE_ID, "2026-10-07")).thenReturn(Collections.singletonList(signed));
        when(courseSpaceMapper.selectMemberIds(COURSE_ID)).thenReturn(Arrays.asList(1, 2, 3));
        when(applyMapper.selectAll(any())).thenReturn(Collections.singletonList(leave(2, "2026-10-06", 2)));

        Map<String, Object> result = service.finish(COURSE_ID, 7);

        assertEquals(2, result.get("marked"));
        ArgumentCaptor<Attendance> saved = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceMapper, times(2)).insert(saved.capture());
        List<Attendance> records = saved.getAllValues();
        assertEquals(2, records.get(0).getStudentId());
        assertEquals("请假", records.get(0).getStatus());
        assertEquals(3, records.get(1).getStudentId());
        assertEquals("缺勤", records.get(1).getStatus());
    }

    @Test
    @DisplayName("并发结束：状态已被别的请求改掉时不重复补记")
    void finishIsIdempotent() {
        when(sessionMapper.selectById(7)).thenReturn(session(7, "2026-10-07 10:05:00"));
        when(sessionMapper.finish(eq(7), anyString())).thenReturn(0);

        Map<String, Object> result = service.finish(COURSE_ID, 7);

        assertEquals(0, result.get("marked"));
        verify(attendanceMapper, never()).insert(any());
    }

    @Test
    @DisplayName("别的课的场次不能拿来结束")
    void cannotFinishOtherCoursesSession() {
        AttendanceSession other = session(7, "2026-10-07 10:05:00");
        other.setCourseId(99);
        when(sessionMapper.selectById(7)).thenReturn(other);

        assertThrows(CustomException.class, () -> service.finish(COURSE_ID, 7));
        verify(sessionMapper, never()).finish(anyInt(), anyString());
    }

    @Test
    @DisplayName("重复点「发起签到」：已有进行中的就返回那一场，不再开新的")
    void startReturnsActiveSession() {
        when(sessionMapper.selectActiveByCourse(COURSE_ID)).thenReturn(session(7, "2026-10-07 10:05:00"));

        Map<String, Object> view = service.start(COURSE_ID, 5);

        assertEquals(7, view.get("id"));
        assertEquals("0427", view.get("code"));
        verify(sessionMapper, never()).insert(any());
    }

    @Test
    @DisplayName("发起签到：4 位数字码，时长限制在 1~30 分钟")
    void startCreatesSession() {
        Map<String, Object> view = service.start(COURSE_ID, 120);

        ArgumentCaptor<AttendanceSession> saved = ArgumentCaptor.forClass(AttendanceSession.class);
        verify(sessionMapper).insert(saved.capture());
        assertTrue(saved.getValue().getCode().matches("\\d{4}"));
        assertEquals("2026-10-07", saved.getValue().getDate());
        assertEquals("2026-10-07 10:00:00", saved.getValue().getStartTime());
        assertEquals("2026-10-07 10:30:00", saved.getValue().getExpireTime());
        assertEquals(saved.getValue().getCode(), view.get("code"));
    }

    @Test
    @DisplayName("请假覆盖判断：从 time 当天起共 day 天，只认审核通过的（调用方已按状态筛选）")
    void leaveCoverage() {
        List<Apply> leaves = Collections.singletonList(leave(2, "2026-10-06", 2));
        assertTrue(AttendanceSessionService.onLeave(leaves, 2, "2026-10-06"));
        assertTrue(AttendanceSessionService.onLeave(leaves, 2, "2026-10-07"));
        assertFalse(AttendanceSessionService.onLeave(leaves, 2, "2026-10-08"));
        assertFalse(AttendanceSessionService.onLeave(leaves, 2, "2026-10-05"));
        assertFalse(AttendanceSessionService.onLeave(leaves, 3, "2026-10-07"));
        assertFalse(AttendanceSessionService.onLeave(leaves, 2, "not-a-date"));
        assertFalse(AttendanceSessionService.onLeave(Collections.singletonList(leave(2, "坏数据", 2)), 2, "2026-10-07"));
    }

    private static AttendanceSession session(int id, String expireTime) {
        AttendanceSession s = new AttendanceSession();
        s.setId(id);
        s.setCourseId(COURSE_ID);
        s.setTeacherId(5);
        s.setCode("0427");
        s.setDate("2026-10-07");
        s.setStartTime("2026-10-07 09:55:00");
        s.setExpireTime(expireTime);
        s.setStatus(AttendanceSession.STATUS_ACTIVE);
        return s;
    }

    private static Apply leave(int studentId, String from, int days) {
        Apply a = new Apply();
        a.setStudentId(studentId);
        a.setTime(from);
        a.setDay(days);
        a.setStatus("审核通过");
        return a;
    }

    private static String codeOf(Runnable action) {
        CustomException e = assertThrows(CustomException.class, action::run);
        return e.getCode();
    }
}
