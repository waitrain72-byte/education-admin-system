package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.exception.CustomException;
import com.example.mapper.AdminMapper;
import com.example.mapper.PeopleMapper;
import com.example.mapper.StudentMapper;
import com.example.mapper.TeacherMapper;
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
 * 删账号前的检查：还有课或带班的老师、还有选课的学生、当前登录的管理员自己，都不能删。
 */
@ExtendWith(MockitoExtension.class)
class AccountDeleteGuardTest {

    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private AdminMapper adminMapper;
    @Mock
    private PeopleMapper peopleMapper;

    @InjectMocks
    private TeacherService teacherService;
    @InjectMocks
    private StudentService studentService;
    @InjectMocks
    private AdminService adminService;

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    @Test
    @DisplayName("还有课程或带着班的老师不能删；都没有的可以")
    void teacherInUseCannotBeDeleted() {
        when(peopleMapper.countCoursesOfTeacher(2)).thenReturn(3);
        when(peopleMapper.countCoursesOfTeacher(4)).thenReturn(0);
        when(peopleMapper.countClassesOfTeacher(4)).thenReturn(1);
        when(peopleMapper.countCoursesOfTeacher(6)).thenReturn(0);
        when(peopleMapper.countClassesOfTeacher(6)).thenReturn(0);

        CustomException e = assertThrows(CustomException.class, () -> teacherService.deleteById(2));
        assertEquals(ResultCodeEnum.TEACHER_IN_USE_ERROR.code, e.getCode());
        assertThrows(CustomException.class, () -> teacherService.deleteById(4));
        teacherService.deleteById(6);

        verify(teacherMapper, never()).deleteById(2);
        verify(teacherMapper, never()).deleteById(4);
        verify(teacherMapper).deleteById(6);
    }

    @Test
    @DisplayName("还有选课记录的学生不能删；批量删除逐条检查")
    void studentWithChoicesCannotBeDeleted() {
        when(peopleMapper.countChoicesOfStudent(10)).thenReturn(0);
        when(peopleMapper.countChoicesOfStudent(11)).thenReturn(4);

        CustomException e = assertThrows(CustomException.class, () -> studentService.deleteBatch(Arrays.asList(10, 11)));
        assertEquals(ResultCodeEnum.STUDENT_IN_USE_ERROR.code, e.getCode());
        verify(studentMapper, never()).deleteById(11);
    }

    @Test
    @DisplayName("管理员不能删自己，可以删别的管理员")
    void adminCannotDeleteSelf() {
        CurrentUser.as("ADMIN", 1, "管理员");

        CustomException e = assertThrows(CustomException.class, () -> adminService.deleteById(1));
        assertEquals(ResultCodeEnum.DELETE_SELF_ERROR.code, e.getCode());
        verify(adminMapper, never()).deleteById(anyInt());

        adminService.deleteById(3);
        verify(adminMapper).deleteById(3);
    }
}
