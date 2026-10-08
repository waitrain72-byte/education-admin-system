package com.example.service;

import com.example.common.AppTime;
import com.example.entity.Assignment;
import com.example.entity.Course;
import com.example.entity.GradebookForm;
import com.example.entity.Homework;
import com.example.entity.Score;
import com.example.entity.Student;
import com.example.exception.CustomException;
import com.example.mapper.AssignmentMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.CourseSpaceMapper;
import com.example.mapper.ScoreMapper;
import com.example.mapper.StudentMapper;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成绩册：总评重算、草稿 / 发布、学分随「是否计入」增减、通知名单。
 */
@ExtendWith(MockitoExtension.class)
class GradebookServiceTest {

    private static final int COURSE_ID = 10;
    private static final int CREDIT = 3;
    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    @Mock
    private CourseSpaceService courseSpaceService;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private ScoreMapper scoreMapper;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private CourseSpaceMapper courseSpaceMapper;
    @Mock
    private AssignmentMapper assignmentMapper;

    @InjectMocks
    private GradebookService service;

    private Course course;

    @BeforeEach
    void setUp() {
        // 2026-10-07 10:00（北京时间）
        AppTime.use(Clock.fixed(Instant.parse("2026-10-07T02:00:00Z"), BEIJING));
        course = new Course();
        course.setId(COURSE_ID);
        course.setName("数据结构");
        course.setScore(CREDIT);
        course.setTeacherId(5);
        lenient().when(courseSpaceService.requireCourse(COURSE_ID)).thenReturn(course);
        // 默认：没有考勤记录（考勤分按全勤 100）、没有作业（作业分为空）
        lenient().when(courseSpaceMapper.attendanceCountsByStudent(COURSE_ID)).thenReturn(Collections.emptyList());
        lenient().when(assignmentMapper.selectByCourse(COURSE_ID)).thenReturn(Collections.emptyList());
        lenient().when(courseSpaceMapper.selectSubmissionsOfCourse(COURSE_ID)).thenReturn(Collections.emptyList());
    }

    @AfterEach
    void tearDown() {
        AppTime.use(Clock.system(BEIJING));
        CurrentUser.clear();
    }

    @Test
    @DisplayName("成绩册：按实时考勤分和录入的平时、期末分算总评，汇总只算有总评的")
    void viewComputesTotalsAndStats() {
        when(courseSpaceMapper.selectMembers(COURSE_ID)).thenReturn(Arrays.asList(student(1), student(2)));
        when(scoreMapper.selectByCourse(COURSE_ID)).thenReturn(Collections.singletonList(row(1, 80.0, 90.0, 87.0, "已发布")));

        Map<String, Object> data = service.view(COURSE_ID);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("rows");
        assertEquals(87.0, rows.get(0).get("total"));
        assertEquals(87.0, rows.get(0).get("publishedTotal"));
        assertEquals(100.0, rows.get(0).get("attendanceScore"));
        assertNull(rows.get(1).get("total"));
        assertNull(rows.get(1).get("status"));

        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) data.get("stats");
        assertEquals(2, stats.get("students"));
        assertEquals(1, stats.get("graded"));
        assertEquals(1, stats.get("published"));
        assertEquals(87.0, stats.get("average"));
        assertEquals(100.0, stats.get("passRate"));
        assertArrayEquals(new int[]{0, 1, 0, 0, 0}, (int[]) stats.get("distribution"));
    }

    @Test
    @DisplayName("保存：权重合计不是 100 直接拒绝，什么都不写")
    void saveRejectsInvalidWeights() {
        GradebookForm form = new GradebookForm();
        form.setWeights(weights(10, 20, 20, 49));

        assertThrows(CustomException.class, () -> service.save(COURSE_ID, form));
        verify(courseMapper, never()).updateById(any());
        verify(scoreMapper, never()).insertGradebookRow(any());
    }

    @Test
    @DisplayName("保存：成绩超出 0~100 拒绝")
    void saveRejectsOutOfRangeScore() {
        GradebookForm form = new GradebookForm();
        form.setRows(Collections.singletonList(input(1, 101.0, 80.0)));

        assertThrows(CustomException.class, () -> service.save(COURSE_ID, form));
        verify(scoreMapper, never()).insertGradebookRow(any());
    }

    @Test
    @DisplayName("保存：新权重写回课程，第一次录的学生建草稿行，不动学分")
    void saveCreatesDraftRowsWithNewWeights() {
        when(courseSpaceMapper.selectMemberIds(COURSE_ID)).thenReturn(Arrays.asList(1, 2));
        when(scoreMapper.selectByCourse(COURSE_ID)).thenReturn(new ArrayList<>());
        GradebookForm form = new GradebookForm();
        form.setWeights(weights(0, 0, 40, 60));
        form.setRows(Collections.singletonList(input(1, 70.0, 80.0)));

        GradebookService.Outcome outcome = service.save(COURSE_ID, form);

        ArgumentCaptor<Course> patch = ArgumentCaptor.forClass(Course.class);
        verify(courseMapper).updateById(patch.capture());
        assertEquals(40, patch.getValue().getWeightOrdinary());
        assertEquals(60, patch.getValue().getWeightExam());

        ArgumentCaptor<Score> inserted = ArgumentCaptor.forClass(Score.class);
        verify(scoreMapper).insertGradebookRow(inserted.capture());
        // 70*0.4 + 80*0.6 = 28 + 48 = 76
        assertEquals(76.0, inserted.getValue().getScore());
        assertEquals(GradeCalculator.DRAFT, inserted.getValue().getStatus());
        assertEquals(5, inserted.getValue().getTeacherId());
        // 学生 2 什么都没录，不建空行
        assertEquals(1, outcome.getCount());
        assertTrue(outcome.getNotify().isEmpty());
        verify(studentMapper, never()).addScore(anyInt(), anyInt());
    }

    @Test
    @DisplayName("保存：已发布的成绩改到不及格，扣回学分并通知学生，状态仍是已发布")
    void savingPublishedRowAcrossPassLine() {
        Score published = row(1, 50.0, 72.0, 65.4, "已发布");
        when(courseSpaceMapper.selectMemberIds(COURSE_ID)).thenReturn(Collections.singletonList(1));
        when(scoreMapper.selectByCourse(COURSE_ID)).thenReturn(Collections.singletonList(published));
        GradebookForm form = new GradebookForm();
        form.setRows(Collections.singletonList(input(1, 50.0, 40.0)));

        GradebookService.Outcome outcome = service.save(COURSE_ID, form);

        // 50*0.3 + 40*0.7 = 15 + 28 = 43
        assertEquals(43.0, published.getScore());
        assertEquals(GradeCalculator.PUBLISHED, published.getStatus());
        verify(scoreMapper).updateGradebookRow(published);
        verify(studentMapper).addScore(1, -CREDIT);
        assertEquals(Collections.singletonList(1), outcome.getNotify());
    }

    @Test
    @DisplayName("保存：已发布的成绩被清掉一项、总评变回待录时退回草稿，扣回学分，不通知")
    void clearingPublishedRowRevertsToDraft() {
        Score published = row(1, 80.0, 90.0, 87.0, "已发布");
        when(courseSpaceMapper.selectMemberIds(COURSE_ID)).thenReturn(Collections.singletonList(1));
        when(scoreMapper.selectByCourse(COURSE_ID)).thenReturn(Collections.singletonList(published));
        GradebookForm form = new GradebookForm();
        form.setRows(Collections.singletonList(input(1, 80.0, null)));

        GradebookService.Outcome outcome = service.save(COURSE_ID, form);

        assertNull(published.getScore());
        assertEquals(GradeCalculator.DRAFT, published.getStatus());
        verify(studentMapper).addScore(1, -CREDIT);
        assertTrue(outcome.getNotify().isEmpty());
    }

    @Test
    @DisplayName("保存：值没变的行不写库")
    void unchangedRowsAreNotWritten() {
        when(courseSpaceMapper.selectMemberIds(COURSE_ID)).thenReturn(Collections.singletonList(1));
        when(scoreMapper.selectByCourse(COURSE_ID)).thenReturn(Collections.singletonList(row(1, 80.0, 90.0, 87.0, "已发布")));

        GradebookService.Outcome outcome = service.save(COURSE_ID, new GradebookForm());

        assertEquals(0, outcome.getCount());
        verify(scoreMapper, never()).updateGradebookRow(any());
    }

    @Test
    @DisplayName("发布：有总评的草稿行发布，及格的计入学分；待录的跳过；已发布的不重复处理")
    void publishDraftRows() {
        Score passing = row(1, 80.0, 80.0, 80.0, "草稿");
        Score failing = row(2, 50.0, 50.0, 50.0, "草稿");
        Score alreadyPublished = row(4, 90.0, 90.0, 90.0, "已发布");
        when(courseSpaceMapper.selectMemberIds(COURSE_ID)).thenReturn(Arrays.asList(1, 2, 3, 4));
        when(scoreMapper.selectByCourse(COURSE_ID)).thenReturn(Arrays.asList(passing, failing, alreadyPublished));

        GradebookService.Outcome outcome = service.publish(COURSE_ID);

        assertEquals(2, outcome.getCount());
        assertEquals(1, outcome.getSkipped());
        assertEquals(Arrays.asList(1, 2), outcome.getNotify());
        assertEquals(GradeCalculator.PUBLISHED, passing.getStatus());
        assertEquals(GradeCalculator.PUBLISHED, failing.getStatus());
        verify(studentMapper).addScore(1, CREDIT);
        verify(studentMapper, never()).addScore(2, CREDIT);
        verify(studentMapper, never()).addScore(4, CREDIT);
    }

    @Test
    @DisplayName("撤回：已发布的退回草稿，及格的扣回学分")
    void unpublishRevokesCredit() {
        Score passing = row(1, 80.0, 80.0, 80.0, "已发布");
        Score failing = row(2, 50.0, 50.0, 50.0, "已发布");
        Score draft = row(3, 90.0, 90.0, 90.0, "草稿");
        when(scoreMapper.selectByCourse(COURSE_ID)).thenReturn(Arrays.asList(passing, failing, draft));

        GradebookService.Outcome outcome = service.unpublish(COURSE_ID);

        assertEquals(2, outcome.getCount());
        assertEquals(GradeCalculator.DRAFT, passing.getStatus());
        assertEquals(GradeCalculator.DRAFT, failing.getStatus());
        verify(studentMapper).addScore(1, -CREDIT);
        verify(studentMapper, never()).addScore(2, -CREDIT);
        verify(studentMapper, never()).addScore(3, -CREDIT);
    }

    @Test
    @DisplayName("实时折算：考勤按状态平均，作业按批改与缺交计")
    void livePointsFromRecords() {
        Map<String, Object> normal = new HashMap<>();
        normal.put("studentId", 1);
        normal.put("status", "正常");
        normal.put("total", 3L);
        Map<String, Object> absent = new HashMap<>();
        absent.put("studentId", 1);
        absent.put("status", "缺勤");
        absent.put("total", 1L);
        when(courseSpaceMapper.attendanceCountsByStudent(COURSE_ID)).thenReturn(Arrays.asList(normal, absent));

        Assignment graded = new Assignment();
        graded.setId(100);
        graded.setFullScore(50);
        graded.setDeadline("2026-10-01 23:59");
        when(assignmentMapper.selectByCourse(COURSE_ID)).thenReturn(Collections.singletonList(graded));
        Homework h = new Homework();
        h.setStudentId(1);
        h.setAssignmentId(100);
        h.setStatus("已批改");
        h.setScore("45");
        when(courseSpaceMapper.selectSubmissionsOfCourse(COURSE_ID)).thenReturn(Collections.singletonList(h));

        Map<Integer, Double[]> points = service.livePoints(COURSE_ID, Arrays.asList(1, 2));

        // 学生 1：考勤 (3*100 + 0)/4 = 75，作业 45/50 = 90
        assertArrayEquals(new Double[]{75.0, 90.0}, points.get(1));
        // 学生 2：没有考勤记录按全勤；作业过了截止没交记 0
        assertArrayEquals(new Double[]{100.0, 0.0}, points.get(2));
    }

    @Test
    @DisplayName("学生看自己的成绩：没发布只给考勤分和作业分")
    void studentSeesOnlyLivePointsBeforePublishing() {
        CurrentUser.as("STUDENT", 1, "张三");
        when(courseSpaceService.requireMember(course)).thenReturn(CourseSpaceService.REL_STUDENT);
        when(scoreMapper.selectByCourceIdAndStudentId(COURSE_ID, 1)).thenReturn(row(1, 80.0, 90.0, 87.0, "草稿"));

        Map<String, Object> data = service.mine(COURSE_ID);

        assertFalse((Boolean) data.get("published"));
        assertEquals(100.0, data.get("attendanceScore"));
        assertFalse(data.containsKey("examScore"));
        assertFalse(data.containsKey("total"));
    }

    @Test
    @DisplayName("学生看自己的成绩：发布了给完整的四项、总评和绩点")
    void studentSeesFullGradeAfterPublishing() {
        CurrentUser.as("STUDENT", 1, "张三");
        when(courseSpaceService.requireMember(course)).thenReturn(CourseSpaceService.REL_STUDENT);
        when(scoreMapper.selectByCourceIdAndStudentId(COURSE_ID, 1)).thenReturn(row(1, 80.0, 90.0, 87.0, "已发布"));

        Map<String, Object> data = service.mine(COURSE_ID);

        assertTrue((Boolean) data.get("published"));
        assertEquals(87.0, data.get("total"));
        assertEquals(90.0, data.get("examScore"));
        assertEquals(3.7, (Double) data.get("gradePoint"), 1e-9);
    }

    @Test
    @DisplayName("老师不能走学生的「我的成绩」接口")
    void teacherCannotUseStudentView() {
        when(courseSpaceService.requireMember(course)).thenReturn(CourseSpaceService.REL_TEACHER);

        assertThrows(CustomException.class, () -> service.mine(COURSE_ID));
    }

    private static Student student(int id) {
        Student s = new Student();
        s.setId(id);
        s.setName("学生" + id);
        return s;
    }

    /** 成绩行：考勤分按全勤 100、作业分为空，和默认的实时折算一致（重算时不会因为这两项变动而写库） */
    private static Score row(int studentId, Double ordinary, Double exam, Double total, String status) {
        Score s = new Score();
        s.setId(studentId * 100);
        s.setStudentId(studentId);
        s.setCourseId(COURSE_ID);
        s.setAttendanceScore(100.0);
        s.setOrdinaryScore(ordinary);
        s.setExamScore(exam);
        s.setScore(total);
        s.setStatus(status);
        return s;
    }

    private static GradebookForm.Weights weights(int attendance, int homework, int ordinary, int exam) {
        GradebookForm.Weights w = new GradebookForm.Weights();
        w.setAttendance(attendance);
        w.setHomework(homework);
        w.setOrdinary(ordinary);
        w.setExam(exam);
        return w;
    }

    private static GradebookForm.Row input(int studentId, Double ordinary, Double exam) {
        GradebookForm.Row r = new GradebookForm.Row();
        r.setStudentId(studentId);
        r.setOrdinaryScore(ordinary);
        r.setExamScore(exam);
        return r;
    }
}
