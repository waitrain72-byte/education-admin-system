package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Course;
import com.example.entity.CourseEval;
import com.example.exception.CustomException;
import com.example.mapper.CourseEvalMapper;
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
import org.springframework.dao.DuplicateKeyException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 课程评价：只有结课后的选课学生能评、每人一次、每项 1~5 星；老师看到的汇总不带学生身份。
 */
@ExtendWith(MockitoExtension.class)
class CourseEvalServiceTest {

    private static final int COURSE_ID = 10;
    private static final int STUDENT_ID = 1;

    @Mock
    private CourseSpaceService courseSpaceService;
    @Mock
    private CourseEvalMapper courseEvalMapper;
    @Mock
    private CourseSpaceMapper courseSpaceMapper;

    @InjectMocks
    private CourseEvalService service;

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(COURSE_ID);
        course.setTeacherId(5);
        course.setStatus("已结课");
        lenient().when(courseSpaceService.requireCourse(COURSE_ID)).thenReturn(course);
    }

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    private void asStudent() {
        CurrentUser.as("STUDENT", STUDENT_ID, "张三");
        when(courseSpaceService.relationOf(course)).thenReturn(CourseSpaceService.REL_STUDENT);
    }

    @Test
    @DisplayName("结课后评价：五项星级 + 文字，记下任课教师")
    void submitAfterCourseEnds() {
        asStudent();

        service.submit(COURSE_ID, eval(5, 4, 4, 5, 3, "  讲得很清楚  "));

        ArgumentCaptor<CourseEval> saved = ArgumentCaptor.forClass(CourseEval.class);
        verify(courseEvalMapper).insert(saved.capture());
        assertEquals(5, saved.getValue().getTeacherId());
        assertEquals(STUDENT_ID, saved.getValue().getStudentId());
        assertEquals("讲得很清楚", saved.getValue().getComment());
    }

    @Test
    @DisplayName("还没结课不能评")
    void notOpenBeforeCourseEnds() {
        asStudent();
        course.setStatus("已开课");

        assertEquals(ResultCodeEnum.EVAL_NOT_OPEN_ERROR.code,
                codeOf(() -> service.submit(COURSE_ID, eval(5, 5, 5, 5, 5, null))));
    }

    @Test
    @DisplayName("每人每门课只能评一次（含并发重复提交被唯一索引拦下的情况）")
    void onlyOncePerStudent() {
        asStudent();
        when(courseEvalMapper.selectByCourseAndStudent(COURSE_ID, STUDENT_ID)).thenReturn(new CourseEval());
        assertEquals(ResultCodeEnum.EVAL_ALREADY_ERROR.code,
                codeOf(() -> service.submit(COURSE_ID, eval(5, 5, 5, 5, 5, null))));
        verify(courseEvalMapper, never()).insert(any());
    }

    @Test
    @DisplayName("并发重复提交：唯一索引报重复时转成「已评价过」")
    void duplicateKeyBecomesAlreadyEvaluated() {
        asStudent();
        doThrow(new DuplicateKeyException("uk_eval_course_student")).when(courseEvalMapper).insert(any());

        assertEquals(ResultCodeEnum.EVAL_ALREADY_ERROR.code,
                codeOf(() -> service.submit(COURSE_ID, eval(5, 5, 5, 5, 5, null))));
    }

    @Test
    @DisplayName("星级超出 1~5 或缺项：参数错误")
    void starsMustBeOneToFive() {
        asStudent();
        assertEquals(ResultCodeEnum.PARAM_ERROR.code,
                codeOf(() -> service.submit(COURSE_ID, eval(6, 5, 5, 5, 5, null))));
        assertEquals(ResultCodeEnum.PARAM_ERROR.code,
                codeOf(() -> service.submit(COURSE_ID, eval(0, 5, 5, 5, 5, null))));
        assertEquals(ResultCodeEnum.PARAM_ERROR.code,
                codeOf(() -> service.submit(COURSE_ID, eval(null, 5, 5, 5, 5, null))));
    }

    @Test
    @DisplayName("老师不能给自己的课打分")
    void teacherCannotSubmit() {
        when(courseSpaceService.relationOf(course)).thenReturn(CourseSpaceService.REL_TEACHER);

        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code,
                codeOf(() -> service.submit(COURSE_ID, eval(5, 5, 5, 5, 5, null))));
    }

    @Test
    @DisplayName("汇总：各维度均分、综合分、星级分布")
    void summary() {
        List<CourseEval> evals = Arrays.asList(eval(5, 5, 5, 5, 5, null), eval(3, 4, 2, 3, 3, null));

        Map<String, Object> summary = CourseEvalService.summarize(evals);

        @SuppressWarnings("unchecked")
        Map<String, Object> averages = (Map<String, Object>) summary.get("averages");
        assertEquals(4.0, averages.get("attitude"));
        assertEquals(4.5, averages.get("contentScore"));
        assertEquals(3.5, averages.get("method"));
        // 综合：(5 + 3) / 2 = 4
        assertEquals(4.0, summary.get("overall"));
        assertArrayEquals(new int[]{0, 0, 1, 0, 1}, (int[]) summary.get("stars"));

        Map<String, Object> empty = CourseEvalService.summarize(Collections.emptyList());
        assertNull(empty.get("overall"));
    }

    @Test
    @DisplayName("老师看到的文字评价是匿名的，不带学生信息")
    void teacherViewIsAnonymous() {
        when(courseSpaceService.requireMember(course)).thenReturn(CourseSpaceService.REL_TEACHER);
        CourseEval withComment = eval(4, 4, 4, 4, 4, "希望多讲例题");
        withComment.setStudentId(STUDENT_ID);
        when(courseEvalMapper.selectByCourse(COURSE_ID)).thenReturn(Arrays.asList(withComment, eval(5, 5, 5, 5, 5, " ")));

        Map<String, Object> data = service.view(COURSE_ID);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> comments = (List<Map<String, Object>>) data.get("comments");
        assertEquals(1, comments.size());
        assertEquals("希望多讲例题", comments.get(0).get("comment"));
        assertFalse(comments.get(0).containsKey("studentId"));
        assertEquals(2, data.get("count"));
    }

    private static CourseEval eval(Integer attitude, Integer content, Integer method, Integer effect, Integer support,
                                   String comment) {
        CourseEval e = new CourseEval();
        e.setAttitude(attitude);
        e.setContentScore(content);
        e.setMethod(method);
        e.setEffect(effect);
        e.setSupport(support);
        e.setComment(comment);
        return e;
    }

    private static String codeOf(Runnable action) {
        CustomException e = assertThrows(CustomException.class, action::run);
        return e.getCode();
    }
}
