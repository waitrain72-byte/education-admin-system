package com.example.service;

import com.example.common.enums.ResultCodeEnum;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学业预警提醒：老师只能提醒选了自己课的学生，管理员不限。
 */
@ExtendWith(MockitoExtension.class)
class WarningNotifyScopeTest {

    @Mock
    private ChoiceMapper choiceMapper;

    @InjectMocks
    private WarningService service;

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    @Test
    @DisplayName("老师提醒别人的学生 → 403；自己的学生可以")
    void teacherOnlyOwnStudents() {
        CurrentUser.as("TEACHER", 3, "陈敏");
        when(choiceMapper.countByStudentAndTeacher(2, 3)).thenReturn(0);
        when(choiceMapper.countByStudentAndTeacher(4, 3)).thenReturn(1);

        CustomException e = assertThrows(CustomException.class, () -> service.requireCanNotify(2));
        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, e.getCode());
        assertDoesNotThrow(() -> service.requireCanNotify(4));
    }

    @Test
    @DisplayName("管理员可以提醒任何学生")
    void adminUnrestricted() {
        CurrentUser.as("ADMIN", 1, "管理员");

        assertDoesNotThrow(() -> service.requireCanNotify(2));
        verify(choiceMapper, never()).countByStudentAndTeacher(anyInt(), anyInt());
    }
}
