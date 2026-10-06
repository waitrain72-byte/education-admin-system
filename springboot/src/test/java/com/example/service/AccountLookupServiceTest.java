package com.example.service;

import com.example.common.enums.RoleEnum;
import com.example.entity.Admin;
import com.example.entity.Student;
import com.example.entity.Teacher;
import com.example.exception.CustomException;
import com.example.mapper.AdminMapper;
import com.example.mapper.StudentMapper;
import com.example.mapper.TeacherMapper;
import com.example.utils.PasswordUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * 登录不选身份：按用户名在三张账号表里判断是谁，以及用户名全局唯一校验。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountLookupServiceTest {

    @Mock
    private AdminMapper adminMapper;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private StudentMapper studentMapper;

    @InjectMocks
    private AccountLookupService service;

    private static final String HASH_123456 = PasswordUtils.encrypt("123456");
    private static final String HASH_OTHER = PasswordUtils.encrypt("abcdef");

    private Teacher teacher(int id, String password) {
        Teacher t = new Teacher();
        t.setId(id);
        t.setUsername("dup");
        t.setPassword(password);
        return t;
    }

    private Student student(int id, String password) {
        Student s = new Student();
        s.setId(id);
        s.setUsername("dup");
        s.setPassword(password);
        return s;
    }

    @Test
    @DisplayName("三张表都没有这个用户名：报 5004 用户不存在")
    void unknownUsername() {
        CustomException e = assertThrows(CustomException.class, () -> service.resolveRole("nobody", "123456"));
        assertEquals("5004", e.getCode());
    }

    @Test
    @DisplayName("只有一张表有：直接返回该身份，密码交给正常登录流程校验")
    void singleMatchSkipsPasswordCheck() {
        when(studentMapper.selectByUsername("zhangsan")).thenReturn(student(1, HASH_OTHER));
        assertEquals("STUDENT", service.resolveRole("zhangsan", "wrong-password"));
    }

    @Test
    @DisplayName("教师表和学生表同名：密码只对得上其中一个，就用那个身份")
    void duplicateResolvedByPassword() {
        when(teacherMapper.selectByUsername("dup")).thenReturn(teacher(2, HASH_OTHER));
        when(studentMapper.selectByUsername("dup")).thenReturn(student(3, HASH_123456));
        assertEquals("STUDENT", service.resolveRole("dup", "123456"));
        assertEquals("TEACHER", service.resolveRole("dup", "abcdef"));
    }

    @Test
    @DisplayName("同名账号密码都不对：报 5003 账号或密码错误")
    void duplicateWithWrongPassword() {
        when(teacherMapper.selectByUsername("dup")).thenReturn(teacher(2, HASH_OTHER));
        when(studentMapper.selectByUsername("dup")).thenReturn(student(3, HASH_123456));
        CustomException e = assertThrows(CustomException.class, () -> service.resolveRole("dup", "nope"));
        assertEquals("5003", e.getCode());
    }

    @Test
    @DisplayName("同名账号密码也相同：报 5012，让前端补选身份")
    void duplicateWithSamePasswordNeedsRole() {
        when(teacherMapper.selectByUsername("dup")).thenReturn(teacher(2, HASH_123456));
        when(studentMapper.selectByUsername("dup")).thenReturn(student(3, "123456"));
        CustomException e = assertThrows(CustomException.class, () -> service.resolveRole("dup", "123456"));
        assertEquals("5012", e.getCode());
    }

    @Test
    @DisplayName("用户名唯一校验：别的表里有同名账号即算占用")
    void takenAcrossTables() {
        Admin admin = new Admin();
        admin.setId(1);
        admin.setUsername("admin");
        when(adminMapper.selectByUsername("admin")).thenReturn(admin);
        assertTrue(service.isUsernameTaken("admin", null, null));
        assertTrue(service.isUsernameTaken("admin", RoleEnum.STUDENT, 1));
    }

    @Test
    @DisplayName("用户名唯一校验：修改时把自己排除在外，空用户名不算占用")
    void selfIsNotTaken() {
        when(studentMapper.selectByUsername("zhangsan")).thenReturn(student(1, HASH_123456));
        assertFalse(service.isUsernameTaken("zhangsan", RoleEnum.STUDENT, 1));
        assertTrue(service.isUsernameTaken("zhangsan", RoleEnum.STUDENT, 2));
        assertFalse(service.isUsernameTaken(" ", null, null));
    }
}
