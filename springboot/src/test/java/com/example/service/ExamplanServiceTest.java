package com.example.service;

import com.example.entity.Course;
import com.example.entity.Examplan;
import com.example.exception.CustomException;
import com.example.mapper.CourseMapper;
import com.example.mapper.ExamplanMapper;
import com.example.support.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 考试安排：考试时间校验（Web 与小程序据考试时间显示倒计时，格式必须是两端都能解析的 yyyy-MM-dd HH:mm），
 * 所属课程校验，以及列表按登录身份收窄的可见范围。
 */
class ExamplanServiceTest {

    private ExamplanService service;
    private ExamplanMapper mapper;
    private CourseMapper courseMapper;

    @BeforeEach
    void setUp() {
        service = new ExamplanService();
        mapper = mock(ExamplanMapper.class);
        courseMapper = mock(CourseMapper.class);
        ReflectionTestUtils.setField(service, "examplanMapper", mapper);
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
    }

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    private Examplan plan(String examTime) {
        Examplan e = new Examplan();
        e.setId(3);
        e.setName("期末考试");
        e.setContent("闭卷");
        e.setExamTime(examTime);
        return e;
    }

    @Test
    @DisplayName("考试时间为空时放行（历史数据），填写了必须是真实存在的 yyyy-MM-dd HH:mm")
    void examTimeFormat() {
        assertTrue(ExamplanService.isValidExamTime(null));
        assertTrue(ExamplanService.isValidExamTime(""));
        assertTrue(ExamplanService.isValidExamTime("2026-12-20 09:00"));
        assertTrue(ExamplanService.isValidExamTime("2028-02-29 23:59"));

        assertFalse(ExamplanService.isValidExamTime("2026-12-20"));
        assertFalse(ExamplanService.isValidExamTime("2026-12-20 09:00:00"));
        assertFalse(ExamplanService.isValidExamTime("2026/12/20 09:00"));
        assertFalse(ExamplanService.isValidExamTime("2026-12-20 9:00"));
        assertFalse(ExamplanService.isValidExamTime("2026-02-30 09:00"));
        assertFalse(ExamplanService.isValidExamTime("2027-02-29 09:00"));
        assertFalse(ExamplanService.isValidExamTime("2026-12-20 24:00"));
        assertFalse(ExamplanService.isValidExamTime("明天上午"));
    }

    @Test
    @DisplayName("新增：考试时间非法时拒绝且不入库；合法时生成发布时间后入库")
    void addValidatesExamTime() {
        assertThrows(CustomException.class, () -> service.add(plan("2026-13-01 09:00")));
        verify(mapper, never()).insert(any());

        Examplan ok = plan("2026-12-20 09:00");
        service.add(ok);
        verify(mapper).insert(ok);
        assertNotNull(ok.getTime());
        assertEquals("2026-12-20 09:00", ok.getExamTime());
    }

    @Test
    @DisplayName("修改：考试时间非法时拒绝；发布时间只在新增时生成，修改时不允许改写")
    void updateValidatesExamTime() {
        assertThrows(CustomException.class, () -> service.updateById(plan("2026-12-20")));
        verify(mapper, never()).updateById(any());

        Examplan ok = plan("2026-12-20 09:00");
        ok.setTime("2020-01-01 00:00:00");
        service.updateById(ok);
        verify(mapper).updateById(ok);
        assertNull(ok.getTime());
        assertEquals("2026-12-20 09:00", ok.getExamTime());
    }

    @Test
    @DisplayName("所属课程：填了不存在的课程就拒绝、不入库；不填是全校考试，不用查课程")
    void courseMustExist() {
        when(courseMapper.selectById(99)).thenReturn(null);
        when(courseMapper.selectById(7)).thenReturn(new Course());

        Examplan ghost = plan("2026-12-20 09:00");
        ghost.setCourseId(99);
        assertThrows(CustomException.class, () -> service.add(ghost));
        assertThrows(CustomException.class, () -> service.updateById(ghost));
        verify(mapper, never()).insert(any());
        verify(mapper, never()).updateById(any());

        Examplan midterm = plan("2026-10-21 14:00");
        midterm.setCourseId(7);
        service.add(midterm);
        verify(mapper).insert(midterm);

        Examplan schoolWide = plan("2026-12-12 09:00");
        service.updateById(schoolWide);
        verify(mapper).updateById(schoolWide);
        verify(courseMapper, never()).selectById(null);
    }

    @Test
    @DisplayName("列表可见范围按登录身份：管理员不收窄，教师按任课、学生按选课收窄；前端传来的范围一律被覆盖")
    void dataScopeFollowsLogin() {
        Examplan probe = new Examplan();
        probe.setScopeStudentId(5);
        probe.setScopeTeacherId(6);

        CurrentUser.as("ADMIN", 1, "管理员");
        service.applyDataScope(probe);
        assertNull(probe.getScopeStudentId());
        assertNull(probe.getScopeTeacherId());

        CurrentUser.as("TEACHER", 2, "路易斯");
        probe.setScopeStudentId(5);
        service.applyDataScope(probe);
        assertNull(probe.getScopeStudentId());
        assertEquals(2, probe.getScopeTeacherId());

        CurrentUser.as("STUDENT", 3, "张三");
        probe.setScopeTeacherId(6);
        service.applyDataScope(probe);
        assertEquals(3, probe.getScopeStudentId());
        assertNull(probe.getScopeTeacherId());

        // 登录信息里没有 id：不能当成「不收窄」放行全部考试
        CurrentUser.as("STUDENT", null, "张三");
        assertThrows(CustomException.class, () -> service.applyDataScope(probe));
    }

    @Test
    @DisplayName("按 ID 查：全校考试人人可查；课程考试不在自己范围内的返回空，管理员不受限")
    void selectByIdFollowsScope() {
        Examplan schoolWide = plan("2026-12-12 09:00");
        schoolWide.setId(6);
        Examplan midterm = plan("2026-10-21 14:00");
        midterm.setId(5);
        midterm.setCourseId(9);
        when(mapper.selectById(6)).thenReturn(schoolWide);
        when(mapper.selectById(5)).thenReturn(midterm);

        CurrentUser.as("STUDENT", 2, "李四");
        assertEquals(schoolWide, service.selectById(6));
        when(mapper.selectAll(any())).thenReturn(java.util.Collections.emptyList());
        assertNull(service.selectById(5));

        CurrentUser.as("STUDENT", 1, "张三");
        when(mapper.selectAll(any())).thenReturn(java.util.Collections.singletonList(midterm));
        assertEquals(midterm, service.selectById(5));

        CurrentUser.as("ADMIN", 1, "管理员");
        assertEquals(midterm, service.selectById(5));
    }
}
