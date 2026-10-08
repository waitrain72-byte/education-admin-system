package com.example.service;

import com.example.common.AppTime;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Assignment;
import com.example.entity.Course;
import com.example.entity.Homework;
import com.example.exception.CustomException;
import com.example.mapper.AssignmentMapper;
import com.example.mapper.CourseSpaceMapper;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 作业任务：布置时的校验、学生提交的时间与状态限制、批改分数范围、附件地址只认本系统上传的。
 */
@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    private static final int COURSE_ID = 10;
    private static final int STUDENT_ID = 1;
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    @Mock
    private CourseSpaceService courseSpaceService;
    @Mock
    private AssignmentMapper assignmentMapper;
    @Mock
    private HomeworkMapper homeworkMapper;
    @Mock
    private CourseSpaceMapper courseSpaceMapper;
    @Spy
    private UploadedFiles uploadedFiles = new UploadedFiles();

    @InjectMocks
    private AssignmentService service;

    private Course course;

    @BeforeEach
    void setUp() {
        // 2026-10-07 10:00（北京时间）
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
    @DisplayName("截止前提交：建一条「已提交」记录，挂在作业任务和任课教师下")
    void submitBeforeDeadline() {
        asStudent();
        when(assignmentMapper.selectById(100)).thenReturn(assignment(100, "2026-10-08 23:59", 100));

        service.submit(COURSE_ID, 100, submission(" 我的答案 ", "/api/files/abc.pdf", "报告.pdf"));

        ArgumentCaptor<Homework> saved = ArgumentCaptor.forClass(Homework.class);
        verify(homeworkMapper).insert(saved.capture());
        Homework h = saved.getValue();
        assertEquals("我的答案", h.getContent());
        assertEquals("/api/files/abc.pdf", h.getFile());
        assertEquals("报告.pdf", h.getFileName());
        assertEquals(AssignmentService.SUBMITTED, h.getStatus());
        assertEquals("2026-10-07 10:00", h.getSubmitTime());
        assertEquals(100, h.getAssignmentId());
        assertEquals(5, h.getTeacherId());
    }

    @Test
    @DisplayName("过了截止时间不能再交")
    void submitAfterDeadlineIsRejected() {
        asStudent();
        when(assignmentMapper.selectById(100)).thenReturn(assignment(100, "2026-10-07 09:59", 100));

        assertEquals(ResultCodeEnum.ASSIGNMENT_CLOSED_ERROR.code,
                codeOf(() -> service.submit(COURSE_ID, 100, submission("答案", null, null))));
    }

    @Test
    @DisplayName("批改过的提交不能再改")
    void gradedSubmissionIsLocked() {
        asStudent();
        when(assignmentMapper.selectById(100)).thenReturn(assignment(100, "2026-10-08 23:59", 100));
        Homework graded = submission("旧答案", null, null);
        graded.setStatus(AssignmentService.GRADED);
        when(courseSpaceMapper.selectSubmission(100, STUDENT_ID)).thenReturn(graded);

        assertEquals(ResultCodeEnum.ASSIGNMENT_GRADED_ERROR.code,
                codeOf(() -> service.submit(COURSE_ID, 100, submission("新答案", null, null))));
        verify(homeworkMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("截止前、批改前改提交：文字和附件都按本次的值覆盖（去掉附件也生效）")
    void resubmitReplacesPreviousSubmission() {
        asStudent();
        when(assignmentMapper.selectById(100)).thenReturn(assignment(100, "2026-10-08 23:59", 100));
        Homework previous = submission("旧答案", "/api/files/old.pdf", "旧.pdf");
        previous.setStatus(AssignmentService.SUBMITTED);
        when(courseSpaceMapper.selectSubmission(100, STUDENT_ID)).thenReturn(previous);

        service.submit(COURSE_ID, 100, submission("新答案", null, null));

        assertEquals("新答案", previous.getContent());
        assertEquals("", previous.getFile());
        assertEquals("", previous.getFileName());
        verify(homeworkMapper).updateById(previous);
    }

    @Test
    @DisplayName("文字和附件都没有：参数缺失")
    void emptySubmissionIsRejected() {
        asStudent();
        when(assignmentMapper.selectById(100)).thenReturn(assignment(100, "2026-10-08 23:59", 100));

        assertEquals(ResultCodeEnum.PARAM_LOST_ERROR.code,
                codeOf(() -> service.submit(COURSE_ID, 100, submission("   ", null, null))));
    }

    @Test
    @DisplayName("附件地址不是本系统上传的（比如 javascript: 链接）一律拒绝")
    void foreignFileUrlIsRejected() {
        asStudent();
        when(assignmentMapper.selectById(100)).thenReturn(assignment(100, "2026-10-08 23:59", 100));

        assertEquals(ResultCodeEnum.PARAM_ERROR.code,
                codeOf(() -> service.submit(COURSE_ID, 100, submission(null, "javascript:alert(1)", "x"))));
        assertEquals(ResultCodeEnum.PARAM_ERROR.code,
                codeOf(() -> service.submit(COURSE_ID, 100, submission(null, "/api/files/../application.yml", "x"))));
        verify(homeworkMapper, never()).insert(any());
    }

    @Test
    @DisplayName("别的课的作业任务不能交到这门课")
    void assignmentMustBelongToCourse() {
        asStudent();
        Assignment other = assignment(100, "2026-10-08 23:59", 100);
        other.setCourseId(99);
        when(assignmentMapper.selectById(100)).thenReturn(other);

        assertEquals(ResultCodeEnum.PARAM_ERROR.code,
                codeOf(() -> service.submit(COURSE_ID, 100, submission("答案", null, null))));
    }

    @Test
    @DisplayName("批改：分数在 0 ~ 满分之间，存成字符串，状态改为已批改")
    void gradeWithinFullScore() {
        Homework h = submission("答案", null, null);
        h.setId(7);
        h.setCourseId(COURSE_ID);
        h.setAssignmentId(100);
        when(homeworkMapper.selectById(7)).thenReturn(h);
        when(assignmentMapper.selectById(100)).thenReturn(assignment(100, "2026-10-01 23:59", 20));

        assertEquals(ResultCodeEnum.HOMEWORK_SCORE_ERROR.code, codeOf(() -> service.grade(COURSE_ID, 7, 25.0, "")));
        service.grade(COURSE_ID, 7, 18.5, "  思路清晰 ");

        assertEquals("18.5", h.getScore());
        assertEquals("思路清晰", h.getDescr());
        assertEquals(AssignmentService.GRADED, h.getStatus());
        verify(homeworkMapper).updateById(h);
    }

    @Test
    @DisplayName("旧版自由提交（没有作业任务）按百分制批改")
    void looseSubmissionUsesHundredPoints() {
        Homework h = submission("答案", null, null);
        h.setId(8);
        h.setCourseId(COURSE_ID);
        when(homeworkMapper.selectById(8)).thenReturn(h);

        service.grade(COURSE_ID, 8, 95.0, null);

        assertEquals("95", h.getScore());
    }

    @Test
    @DisplayName("别的课的提交不能在这门课里批改")
    void cannotGradeOtherCoursesSubmission() {
        Homework h = submission("答案", null, null);
        h.setId(9);
        h.setCourseId(99);
        when(homeworkMapper.selectById(9)).thenReturn(h);

        assertEquals(ResultCodeEnum.PARAM_ERROR.code, codeOf(() -> service.grade(COURSE_ID, 9, 80.0, null)));
    }

    @Test
    @DisplayName("布置作业：标题和截止时间必填，截止时间格式 yyyy-MM-dd HH:mm，满分 1~1000，默认 100")
    void createValidatesForm() {
        Assignment noDeadline = form("实验报告", null, null);
        assertEquals(ResultCodeEnum.PARAM_LOST_ERROR.code, codeOf(() -> service.create(COURSE_ID, noDeadline)));
        Assignment badDeadline = form("实验报告", "2026/10/20 23:59", null);
        assertEquals(ResultCodeEnum.PARAM_ERROR.code, codeOf(() -> service.create(COURSE_ID, badDeadline)));
        Assignment zeroFull = form("实验报告", "2026-10-20 23:59", 0);
        assertEquals(ResultCodeEnum.PARAM_ERROR.code, codeOf(() -> service.create(COURSE_ID, zeroFull)));
        Assignment foreignAttachment = form("实验报告", "2026-10-20 23:59", null);
        foreignAttachment.setAttachment("https://evil.example/x.exe");
        assertEquals(ResultCodeEnum.PARAM_ERROR.code, codeOf(() -> service.create(COURSE_ID, foreignAttachment)));
        verify(assignmentMapper, never()).insert(any());

        Assignment saved = service.create(COURSE_ID, form("  实验报告  ", "2026-10-20 23:59", null));

        verify(assignmentMapper).insert(saved);
        assertEquals("实验报告", saved.getTitle());
        assertEquals(100, saved.getFullScore());
        assertEquals("2026-10-07 10:00", saved.getCreateTime());
        assertEquals(5, saved.getTeacherId());
    }

    @Test
    @DisplayName("学生视角的状态：已批改 / 已提交 / 已逾期 / 未提交")
    void studentStatus() {
        Assignment open = assignment(1, "2026-10-08 23:59", 100);
        Assignment closed = assignment(2, "2026-10-06 23:59", 100);
        Homework graded = submission("a", null, null);
        graded.setStatus(AssignmentService.GRADED);
        Homework submitted = submission("a", null, null);
        submitted.setStatus(AssignmentService.SUBMITTED);
        String now = "2026-10-07 10:00";

        assertEquals(AssignmentService.GRADED, AssignmentService.studentStatus(graded, closed, now));
        assertEquals(AssignmentService.SUBMITTED, AssignmentService.studentStatus(submitted, closed, now));
        assertEquals(AssignmentService.OVERDUE, AssignmentService.studentStatus(null, closed, now));
        assertEquals(AssignmentService.PENDING, AssignmentService.studentStatus(null, open, now));
    }

    @Test
    @DisplayName("分数格式：整数不带小数点，其余保留一位")
    void scoreFormat() {
        assertEquals("18", AssignmentService.format(18.0));
        assertEquals("18.3", AssignmentService.format(18.25));
        assertEquals("0", AssignmentService.format(0));
    }

    private static Assignment assignment(int id, String deadline, int fullScore) {
        Assignment a = new Assignment();
        a.setId(id);
        a.setCourseId(COURSE_ID);
        a.setTitle("作业" + id);
        a.setDeadline(deadline);
        a.setFullScore(fullScore);
        return a;
    }

    private static Assignment form(String title, String deadline, Integer fullScore) {
        Assignment a = new Assignment();
        a.setTitle(title);
        a.setDeadline(deadline);
        a.setFullScore(fullScore);
        return a;
    }

    private static Homework submission(String content, String file, String fileName) {
        Homework h = new Homework();
        h.setContent(content);
        h.setFile(file);
        h.setFileName(fileName);
        return h;
    }

    private static String codeOf(Runnable action) {
        CustomException e = assertThrows(CustomException.class, action::run);
        return e.getCode();
    }
}
