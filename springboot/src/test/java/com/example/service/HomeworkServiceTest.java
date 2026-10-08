package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Course;
import com.example.entity.Homework;
import com.example.exception.CustomException;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.HomeworkMapper;
import com.example.support.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 旧版作业接口的归属与字段限制：作业任务的得分会进成绩册，旧接口不能成为改分的后门。
 */
@ExtendWith(MockitoExtension.class)
class HomeworkServiceTest {

    private static final int COURSE_ID = 10;
    private static final int STUDENT_ID = 1;
    private static final int TEACHER_ID = 5;

    @Mock
    private HomeworkMapper homeworkMapper;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private ChoiceMapper choiceMapper;
    @Mock
    private AssignmentService assignmentService;
    @Spy
    private UploadedFiles uploadedFiles = new UploadedFiles();

    @InjectMocks
    private HomeworkService service;

    @BeforeEach
    void setUp() {
        Course course = new Course();
        course.setId(COURSE_ID);
        course.setTeacherId(TEACHER_ID);
        lenient().when(courseMapper.selectById(COURSE_ID)).thenReturn(course);
    }

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    @Test
    @DisplayName("学生交作业：分数、评语、状态、所属作业任务都不收，学生和老师按登录人和课程定")
    void studentAddIsSanitized() {
        CurrentUser.as("STUDENT", STUDENT_ID, "张三");
        when(choiceMapper.countByStudentAndCourse(STUDENT_ID, COURSE_ID)).thenReturn(1);
        Homework form = homework(null, 99, null, "100");
        form.setCourseId(COURSE_ID);
        form.setDescr("自己写的好评");
        form.setStatus("已批改");
        form.setAssignmentId(3);
        form.setTeacherId(42);
        form.setFile("/api/files/abc.pdf");

        service.add(form);

        ArgumentCaptor<Homework> saved = ArgumentCaptor.forClass(Homework.class);
        verify(homeworkMapper).insert(saved.capture());
        Homework h = saved.getValue();
        assertEquals(STUDENT_ID, h.getStudentId());
        assertEquals(TEACHER_ID, h.getTeacherId());
        assertNull(h.getScore());
        assertNull(h.getDescr());
        assertNull(h.getStatus());
        assertNull(h.getAssignmentId());
    }

    @Test
    @DisplayName("没选这门课的学生不能交")
    void studentMustBeEnrolled() {
        CurrentUser.as("STUDENT", STUDENT_ID, "张三");
        when(choiceMapper.countByStudentAndCourse(STUDENT_ID, COURSE_ID)).thenReturn(0);
        Homework form = homework(null, STUDENT_ID, null, null);
        form.setCourseId(COURSE_ID);

        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, codeOf(() -> service.add(form)));
    }

    @Test
    @DisplayName("附件地址不是本系统上传的：拒绝")
    void foreignFileIsRejected() {
        CurrentUser.as("STUDENT", STUDENT_ID, "张三");
        when(choiceMapper.countByStudentAndCourse(STUDENT_ID, COURSE_ID)).thenReturn(1);
        Homework form = homework(null, STUDENT_ID, null, null);
        form.setCourseId(COURSE_ID);
        form.setFile("javascript:alert(1)");

        assertEquals(ResultCodeEnum.PARAM_ERROR.code, codeOf(() -> service.add(form)));
        verify(homeworkMapper, never()).insert(any());
    }

    @Test
    @DisplayName("学生改自己的作业：只改内容，带上来的分数被忽略")
    void studentUpdateOnlyTouchesContent() {
        CurrentUser.as("STUDENT", STUDENT_ID, "张三");
        when(homeworkMapper.selectById(7)).thenReturn(homework(7, STUDENT_ID, null, null));
        Homework form = homework(7, STUDENT_ID, null, "100");
        form.setContent("改过的答案");

        service.updateById(form);

        ArgumentCaptor<Homework> patch = ArgumentCaptor.forClass(Homework.class);
        verify(homeworkMapper).updateById(patch.capture());
        assertEquals("改过的答案", patch.getValue().getContent());
        assertNull(patch.getValue().getScore());
        assertNull(patch.getValue().getStudentId());
    }

    @Test
    @DisplayName("学生不能改别人的、批改过的、或挂在作业任务下的提交")
    void studentCannotTouchOthersOrGradedOrAssignmentRows() {
        CurrentUser.as("STUDENT", STUDENT_ID, "张三");
        when(homeworkMapper.selectById(7)).thenReturn(homework(7, 2, null, null));
        when(homeworkMapper.selectById(8)).thenReturn(homework(8, STUDENT_ID, null, "60"));
        when(homeworkMapper.selectById(9)).thenReturn(homework(9, STUDENT_ID, 3, null));

        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, codeOf(() -> service.updateById(homework(7, STUDENT_ID, null, null))));
        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, codeOf(() -> service.updateById(homework(8, STUDENT_ID, null, null))));
        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, codeOf(() -> service.deleteById(9)));
        verify(homeworkMapper, never()).updateById(any());
        verify(homeworkMapper, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("老师批改挂在作业任务下的提交：按课程空间的规则走（分数范围、改为已批改）")
    void teacherGradingAssignmentSubmissionDelegates() {
        CurrentUser.as("TEACHER", TEACHER_ID, "王强");
        Homework db = homework(9, STUDENT_ID, 3, null);
        db.setCourseId(COURSE_ID);
        db.setTeacherId(TEACHER_ID);
        when(homeworkMapper.selectById(9)).thenReturn(db);
        Homework form = homework(9, STUDENT_ID, 3, "18.5");
        form.setDescr("不错");

        service.updateById(form);

        verify(assignmentService).grade(COURSE_ID, 9, 18.5, "不错");
        verify(homeworkMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("老师给作业任务的提交打非数字分数：报分数错误")
    void nonNumericScoreForAssignmentIsRejected() {
        CurrentUser.as("TEACHER", TEACHER_ID, "王强");
        Homework db = homework(9, STUDENT_ID, 3, null);
        db.setTeacherId(TEACHER_ID);
        when(homeworkMapper.selectById(9)).thenReturn(db);

        assertEquals(ResultCodeEnum.HOMEWORK_SCORE_ERROR.code, codeOf(() -> service.updateById(homework(9, STUDENT_ID, 3, "优秀"))));
        verify(assignmentService, never()).grade(anyInt(), anyInt(), any(), anyString());
    }

    @Test
    @DisplayName("老师批改直接上传的作业：只写分数和评语")
    void teacherGradesLooseSubmission() {
        CurrentUser.as("TEACHER", TEACHER_ID, "王强");
        Homework db = homework(7, STUDENT_ID, null, null);
        db.setTeacherId(TEACHER_ID);
        when(homeworkMapper.selectById(7)).thenReturn(db);
        Homework form = homework(7, 99, null, "88");
        form.setDescr("思路正确");

        service.updateById(form);

        ArgumentCaptor<Homework> patch = ArgumentCaptor.forClass(Homework.class);
        verify(homeworkMapper).updateById(patch.capture());
        assertEquals("88", patch.getValue().getScore());
        assertEquals("思路正确", patch.getValue().getDescr());
        assertNull(patch.getValue().getStudentId());
    }

    @Test
    @DisplayName("老师不能批改、删除、查看别的老师课上的作业")
    void teacherIsScopedToOwnCourses() {
        CurrentUser.as("TEACHER", TEACHER_ID, "王强");
        Homework other = homework(7, STUDENT_ID, null, null);
        other.setTeacherId(2);
        when(homeworkMapper.selectById(7)).thenReturn(other);

        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, codeOf(() -> service.updateById(homework(7, STUDENT_ID, null, "90"))));
        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, codeOf(() -> service.deleteById(7)));
        assertNull(service.selectById(7));
    }

    @Test
    @DisplayName("批量删除逐条检查归属：碰到一条不是自己的就整体拒绝")
    void batchDeleteChecksEachRow() {
        CurrentUser.as("STUDENT", STUDENT_ID, "张三");
        when(homeworkMapper.selectById(7)).thenReturn(homework(7, STUDENT_ID, null, null));
        when(homeworkMapper.selectById(8)).thenReturn(homework(8, 2, null, null));

        assertThrows(CustomException.class, () -> service.deleteBatch(Arrays.asList(7, 8)));
        verify(homeworkMapper).deleteById(7);
        verify(homeworkMapper, never()).deleteById(eq(8));
    }

    @Test
    @DisplayName("学生按 ID 只看得到自己的作业")
    void studentSelectByIdIsScoped() {
        CurrentUser.as("STUDENT", STUDENT_ID, "张三");
        Homework mine = homework(7, STUDENT_ID, null, null);
        when(homeworkMapper.selectById(7)).thenReturn(mine);
        when(homeworkMapper.selectById(8)).thenReturn(homework(8, 2, null, null));

        assertSame(mine, service.selectById(7));
        assertNull(service.selectById(8));
    }

    private static Homework homework(Integer id, Integer studentId, Integer assignmentId, String score) {
        Homework h = new Homework();
        h.setId(id);
        h.setStudentId(studentId);
        h.setAssignmentId(assignmentId);
        h.setScore(score);
        h.setContent("答案");
        return h;
    }

    private static String codeOf(Runnable action) {
        CustomException e = assertThrows(CustomException.class, action::run);
        return e.getCode();
    }
}
