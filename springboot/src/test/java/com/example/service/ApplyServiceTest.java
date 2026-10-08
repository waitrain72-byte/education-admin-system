package com.example.service;

import com.example.common.AppTime;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Apply;
import com.example.entity.Attendance;
import com.example.exception.CustomException;
import com.example.mapper.ApplyMapper;
import com.example.mapper.AttendanceMapper;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 请假：学生只能提交和改自己没审核的申请、不能自己批准；批准时把已过去的日子补记成请假考勤。
 */
@ExtendWith(MockitoExtension.class)
class ApplyServiceTest {

    private static final int STUDENT_ID = 1;
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    @Mock
    private ApplyMapper applyMapper;
    @Mock
    private AttendanceMapper attendanceMapper;
    @Mock
    private ScheduleService scheduleService;

    @InjectMocks
    private ApplyService service;

    @BeforeEach
    void setUp() {
        // 今天 = 2026-10-08（星期四）
        AppTime.use(Clock.fixed(Instant.parse("2026-10-08T02:00:00Z"), BEIJING));
    }

    @AfterEach
    void tearDown() {
        AppTime.use(Clock.system(BEIJING));
        CurrentUser.clear();
    }

    @Test
    @DisplayName("学生提交：一律以自己的名义、待审核，带上来的状态和审核意见不算数")
    void studentSubmitIsPending() {
        CurrentUser.as("STUDENT", STUDENT_ID, "张三");
        Apply form = apply(null, 99, "2026-10-09", 2, "审核通过");
        form.setDescr("自己批的");

        service.add(form);

        ArgumentCaptor<Apply> saved = ArgumentCaptor.forClass(Apply.class);
        verify(applyMapper).insert(saved.capture());
        assertEquals(STUDENT_ID, saved.getValue().getStudentId());
        assertEquals(ApplyService.PENDING, saved.getValue().getStatus());
        assertNull(saved.getValue().getDescr());
    }

    @Test
    @DisplayName("日期范围：最早补请 7 天前，一次最多 30 天")
    void dateRangeIsLimited() {
        CurrentUser.as("STUDENT", STUDENT_ID, "张三");

        assertEquals(ResultCodeEnum.LEAVE_RANGE_ERROR.code, codeOf(() -> service.add(apply(null, STUDENT_ID, "2026-09-30", 1, null))));
        assertEquals(ResultCodeEnum.LEAVE_RANGE_ERROR.code, codeOf(() -> service.add(apply(null, STUDENT_ID, "2026-10-09", 31, null))));
        assertEquals(ResultCodeEnum.LEAVE_RANGE_ERROR.code, codeOf(() -> service.add(apply(null, STUDENT_ID, "下周一", 1, null))));
        // 正好 7 天前可以
        service.add(apply(null, STUDENT_ID, "2026-10-01", 1, null));
        verify(applyMapper).insert(any());
    }

    @Test
    @DisplayName("学生不能自己批准：带状态的修改只改理由和日期，状态不动")
    void studentCannotApproveOwnLeave() {
        CurrentUser.as("STUDENT", STUDENT_ID, "张三");
        when(applyMapper.selectById(3)).thenReturn(apply(3, STUDENT_ID, "2026-10-09", 1, ApplyService.PENDING));
        Apply form = apply(3, STUDENT_ID, "2026-10-09", 2, ApplyService.APPROVED);
        form.setContent("改成两天");

        boolean reviewed = service.update(form);

        assertFalse(reviewed);
        ArgumentCaptor<Apply> patch = ArgumentCaptor.forClass(Apply.class);
        verify(applyMapper).updateById(patch.capture());
        assertNull(patch.getValue().getStatus());
        assertEquals(2, patch.getValue().getDay());
        verify(attendanceMapper, never()).insert(any());
    }

    @Test
    @DisplayName("已审核的申请学生不能再改、不能撤回；别人的申请更不行")
    void reviewedOrOthersLeaveIsLocked() {
        CurrentUser.as("STUDENT", STUDENT_ID, "张三");
        when(applyMapper.selectById(3)).thenReturn(apply(3, STUDENT_ID, "2026-10-09", 1, ApplyService.APPROVED));
        when(applyMapper.selectById(4)).thenReturn(apply(4, 2, "2026-10-09", 1, ApplyService.PENDING));

        assertEquals(ResultCodeEnum.LEAVE_LOCKED_ERROR.code, codeOf(() -> service.deleteById(3)));
        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, codeOf(() -> service.update(apply(4, STUDENT_ID, "2026-10-09", 1, null))));
        assertNull(service.selectById(4));
        verify(applyMapper, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("老师不能审核请假（审核是管理员的事）")
    void teacherCannotReview() {
        CurrentUser.as("TEACHER", 2, "路易斯");
        when(applyMapper.selectById(3)).thenReturn(apply(3, STUDENT_ID, "2026-10-09", 1, ApplyService.PENDING));

        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code,
                codeOf(() -> service.update(apply(3, STUDENT_ID, null, null, ApplyService.APPROVED))));
    }

    @Test
    @DisplayName("管理员批准：已经过去的日子里，没有考勤的补「请假」，缺勤的改成请假，到了课的不动；还没到的日子不记")
    void approvalMarksPastClassesAsLeave() {
        CurrentUser.as("ADMIN", 1, "管理员");
        // 10-06 ~ 10-10 共 5 天，今天 10-08：只处理 10-06 ~ 10-08
        when(applyMapper.selectById(3)).thenReturn(apply(3, STUDENT_ID, "2026-10-06", 5, ApplyService.PENDING));
        when(scheduleService.classesBetween(STUDENT_ID, LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 8)))
                .thenReturn(Arrays.asList(
                        cls(7, "2026-10-06"),   // 没有记录 → 补请假
                        cls(8, "2026-10-06"),   // 记了缺勤 → 改请假
                        cls(11, "2026-10-08"))); // 到了课 → 不动
        Attendance absent = record("缺勤");
        when(attendanceMapper.selectByStudentIdAndCourseIdAndTime(STUDENT_ID, 7, "2026-10-06")).thenReturn(null);
        when(attendanceMapper.selectByStudentIdAndCourseIdAndTime(STUDENT_ID, 8, "2026-10-06")).thenReturn(absent);
        when(attendanceMapper.selectByStudentIdAndCourseIdAndTime(STUDENT_ID, 11, "2026-10-08")).thenReturn(record("正常"));

        boolean reviewed = service.update(apply(3, null, null, null, ApplyService.APPROVED));

        assertTrue(reviewed);
        ArgumentCaptor<Attendance> inserted = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceMapper).insert(inserted.capture());
        assertEquals(7, inserted.getValue().getCourseId());
        assertEquals("请假", inserted.getValue().getStatus());
        assertEquals(2, inserted.getValue().getTeacherId());
        assertEquals("请假", absent.getStatus());
        verify(attendanceMapper).updateById(absent);
    }

    @Test
    @DisplayName("驳回、或者重复批准同一条：不补记考勤")
    void rejectionDoesNotTouchAttendance() {
        CurrentUser.as("ADMIN", 1, "管理员");
        when(applyMapper.selectById(3)).thenReturn(apply(3, STUDENT_ID, "2026-10-06", 2, ApplyService.PENDING));
        when(applyMapper.selectById(5)).thenReturn(apply(5, STUDENT_ID, "2026-10-06", 2, ApplyService.APPROVED));

        assertTrue(service.update(apply(3, null, null, null, ApplyService.REJECTED)));
        assertFalse(service.update(apply(5, null, null, null, ApplyService.APPROVED)));
        verify(scheduleService, never()).classesBetween(anyInt(), any(), any());
    }

    @Test
    @DisplayName("审核状态只能是三种之一")
    void unknownStatusIsRejected() {
        CurrentUser.as("ADMIN", 1, "管理员");
        when(applyMapper.selectById(3)).thenReturn(apply(3, STUDENT_ID, "2026-10-06", 2, ApplyService.PENDING));

        assertEquals(ResultCodeEnum.PARAM_ERROR.code, codeOf(() -> service.update(apply(3, null, null, null, "随便"))));
        verify(attendanceMapper, never()).selectByStudentIdAndCourseIdAndTime(anyInt(), anyInt(), anyString());
    }

    @Test
    @DisplayName("请假预览只给学生用，按日期区间列课")
    void previewListsClasses() {
        CurrentUser.as("STUDENT", STUDENT_ID, "张三");
        when(scheduleService.classesBetween(STUDENT_ID, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 14)))
                .thenReturn(Arrays.asList(cls(1, "2026-10-12"), cls(7, "2026-10-13")));

        assertEquals(2, service.preview("2026-10-12", 3).size());
    }

    private static Apply apply(Integer id, Integer studentId, String time, Integer day, String status) {
        Apply a = new Apply();
        a.setId(id);
        a.setStudentId(studentId);
        a.setContent("身体不舒服");
        a.setTime(time);
        a.setDay(day);
        a.setStatus(status);
        return a;
    }

    private static Map<String, Object> cls(int courseId, String date) {
        Map<String, Object> row = new HashMap<>();
        row.put("courseId", courseId);
        row.put("date", date);
        row.put("teacherId", 2);
        return row;
    }

    private static Attendance record(String status) {
        Attendance a = new Attendance();
        a.setId(500);
        a.setStatus(status);
        return a;
    }

    private static String codeOf(Runnable action) {
        CustomException e = assertThrows(CustomException.class, action::run);
        return e.getCode();
    }
}
