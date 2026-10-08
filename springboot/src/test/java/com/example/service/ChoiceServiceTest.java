package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Choice;
import com.example.exception.CustomException;
import com.example.mapper.ChoiceMapper;
import com.example.support.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 旧版选课接口：选课规则交给 {@link EnrollmentService}（见 EnrollmentServiceTest），这里只管「替谁选、谁能退」。
 *
 * <p>回归用例：改版前学生的选课请求里带什么 studentId 就给谁选，按 ID 删选课记录也不看是谁的。</p>
 */
@ExtendWith(MockitoExtension.class)
class ChoiceServiceTest {

    private static final int COURSE_ID = 5;
    private static final int STUDENT_ID = 2;

    @Mock
    private ChoiceMapper choiceMapper;
    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private ChoiceService service;

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    private static Choice choice(Integer id, Integer studentId) {
        Choice ch = new Choice();
        ch.setId(id);
        ch.setStudentId(studentId);
        ch.setCourseId(COURSE_ID);
        return ch;
    }

    @Test
    @DisplayName("学生选课：一律给自己选，请求里带的别人的学号不算数")
    void studentAlwaysEnrollsSelf() {
        CurrentUser.as("STUDENT", STUDENT_ID, "李四");

        service.add(choice(null, 99));

        verify(enrollmentService).enroll(STUDENT_ID, COURSE_ID);
    }

    @Test
    @DisplayName("管理员可以替学生选课")
    void adminEnrollsGivenStudent() {
        CurrentUser.as("ADMIN", 1, "管理员");

        service.add(choice(null, 7));

        verify(enrollmentService).enroll(7, COURSE_ID);
    }

    @Test
    @DisplayName("老师不能选课")
    void teacherCannotEnroll() {
        CurrentUser.as("TEACHER", 3, "陈敏");

        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, codeOf(() -> service.add(choice(null, 7))));
        verify(enrollmentService, never()).enroll(anyInt(), anyInt());
    }

    @Test
    @DisplayName("学生只能退自己的选课记录；退选按 EnrollmentService 的规则走")
    void studentDropsOnlyOwnChoice() {
        CurrentUser.as("STUDENT", STUDENT_ID, "李四");
        when(choiceMapper.selectById(10)).thenReturn(choice(10, STUDENT_ID));
        when(choiceMapper.selectById(11)).thenReturn(choice(11, 8));

        service.deleteById(10);
        verify(enrollmentService).drop(STUDENT_ID, COURSE_ID);

        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, codeOf(() -> service.deleteById(11)));
        verify(enrollmentService, never()).drop(8, COURSE_ID);
    }

    @Test
    @DisplayName("批量退选逐条检查：碰到别人的记录就拒绝")
    void batchDropChecksEachRow() {
        CurrentUser.as("STUDENT", STUDENT_ID, "李四");
        when(choiceMapper.selectById(10)).thenReturn(choice(10, STUDENT_ID));
        when(choiceMapper.selectById(11)).thenReturn(choice(11, 8));

        assertThrows(CustomException.class, () -> service.deleteBatch(Arrays.asList(10, 11)));
        verify(enrollmentService).drop(STUDENT_ID, COURSE_ID);
    }

    @Test
    @DisplayName("删除不存在的选课记录：静默返回")
    void deletingMissingChoiceIsNoop() {
        CurrentUser.as("STUDENT", STUDENT_ID, "李四");
        when(choiceMapper.selectById(99)).thenReturn(null);

        service.deleteById(99);

        verify(enrollmentService, never()).drop(anyInt(), anyInt());
    }

    private static String codeOf(Runnable action) {
        CustomException e = assertThrows(CustomException.class, action::run);
        return e.getCode();
    }
}
