package com.example.service;

import com.example.entity.Course;
import com.example.entity.Score;
import com.example.exception.CustomException;
import com.example.mapper.CourseMapper;
import com.example.mapper.ScoreMapper;
import com.example.mapper.StudentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成绩录入的学分记账单元测试。
 *
 * <p>锁死一组曾经存在的缺陷，防止回归：</p>
 * <ul>
 *   <li>新增时「及格才加学分」，但删除时无条件扣学分 —— 删一条不及格成绩会把学分扣成负数；</li>
 *   <li>未覆盖 updateById，改分后总成绩不重算，且及格状态变化时学分不跟着调整。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    /** 课程学分，用于断言学分增减量 */
    private static final int CREDIT = 3;
    private static final int COURSE_ID = 10;
    private static final int STUDENT_ID = 7;

    @Mock
    private ScoreMapper scoreMapper;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private GradebookService gradebookService;

    @InjectMocks
    private ScoreService service;

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(COURSE_ID);
        course.setScore(CREDIT);
        // 总评按课程权重算，新增、修改都要读课程；没设过权重的课程沿用「平时 30 + 期末 70」
        lenient().when(courseMapper.selectById(COURSE_ID)).thenReturn(course);
    }

    private Score score(Integer id, double ordinary, double exam) {
        Score s = new Score();
        s.setId(id);
        s.setStudentId(STUDENT_ID);
        s.setCourseId(COURSE_ID);
        s.setOrdinaryScore(ordinary);
        s.setExamScore(exam);
        return s;
    }

    /** 已落库的成绩行（总成绩已算好） */
    private Score persisted(Integer id, double total) {
        Score s = score(id, total, total);
        s.setScore(total);
        return s;
    }

    // ========== 总成绩计算 ==========

    @Test
    @DisplayName("新增：总成绩 = 平时分 * 30% + 考试分 * 70%")
    void addComputesWeightedTotal() {
        Score input = score(null, 60, 90);

        service.add(input);

        // 60*0.3 + 90*0.7 = 18 + 63 = 81
        assertEquals(81.0, input.getScore());
        verify(scoreMapper).insert(input);
    }

    @Test
    @DisplayName("修改：总成绩按新的平时分/考试分重算（原先沿用父类裸更新会留旧值）")
    void updateRecomputesTotal() {
        // 改分前后都及格，隔离出「总成绩重算」这一个关注点，不牵动学分调整
        when(scoreMapper.selectById(1)).thenReturn(persisted(1, 90));
        Score input = score(1, 60, 80);

        service.updateById(input);

        // 60*0.3 + 80*0.7 = 18 + 56 = 74
        assertEquals(74.0, input.getScore());
        verify(scoreMapper).updateById(input);
        verify(studentMapper, never()).addScore(anyInt(), anyInt());
    }

    @Test
    @DisplayName("修改：只传平时分时，考试分沿用库中原值后再重算")
    void updateFallsBackToPersistedFields() {
        Score db = persisted(1, 80);
        db.setExamScore(90.0);
        when(scoreMapper.selectById(1)).thenReturn(db);

        Score input = new Score();
        input.setId(1);
        input.setOrdinaryScore(60.0);

        service.updateById(input);

        // 60*0.3 + 90*0.7 = 81
        assertEquals(81.0, input.getScore());
    }

    // ========== 学分记账 ==========

    @Test
    @DisplayName("新增及格成绩：累加该课学分")
    void addPassingScoreGrantsCredit() {

        service.add(score(null, 60, 60));

        verify(studentMapper).addScore(STUDENT_ID, CREDIT);
    }

    @Test
    @DisplayName("新增不及格成绩：不加学分")
    void addFailingScoreGrantsNothing() {
        service.add(score(null, 50, 50));

        verify(studentMapper, never()).addScore(anyInt(), anyInt());
    }

    @Test
    @DisplayName("及格线边界：总成绩正好 60 分算及格")
    void exactlySixtyCountsAsPassed() {

        service.add(score(null, 60, 60));

        verify(studentMapper).addScore(STUDENT_ID, CREDIT);
    }

    @Test
    @DisplayName("删除及格成绩：扣减该课学分")
    void deletePassingScoreRevokesCredit() {
        when(scoreMapper.selectById(1)).thenReturn(persisted(1, 85));

        service.deleteById(1);

        verify(scoreMapper).deleteById(1);
        verify(studentMapper).addScore(STUDENT_ID, -CREDIT);
    }

    @Test
    @DisplayName("删除不及格成绩：不扣学分（回归用例：原先无条件扣减会把学分扣成负数）")
    void deleteFailingScoreDoesNotRevokeCredit() {
        when(scoreMapper.selectById(1)).thenReturn(persisted(1, 40));

        service.deleteById(1);

        verify(scoreMapper).deleteById(1);
        verify(studentMapper, never()).addScore(anyInt(), anyInt());
    }

    @Test
    @DisplayName("改分：不及格 → 及格，补发学分")
    void updateFailToPassGrantsCredit() {
        when(scoreMapper.selectById(1)).thenReturn(persisted(1, 40));

        service.updateById(score(1, 90, 90));

        verify(studentMapper).addScore(STUDENT_ID, CREDIT);
    }

    @Test
    @DisplayName("改分：及格 → 不及格，收回学分")
    void updatePassToFailRevokesCredit() {
        when(scoreMapper.selectById(1)).thenReturn(persisted(1, 90));

        service.updateById(score(1, 40, 40));

        verify(studentMapper).addScore(STUDENT_ID, -CREDIT);
    }

    @Test
    @DisplayName("改分：及格状态未变化，学分不动")
    void updateWithinSameStateLeavesCreditAlone() {
        when(scoreMapper.selectById(1)).thenReturn(persisted(1, 70));

        service.updateById(score(1, 90, 90));

        verify(studentMapper, never()).addScore(anyInt(), anyInt());
    }

    @Test
    @DisplayName("批量删除逐条走单条删除逻辑，学分同步调整")
    void deleteBatchAdjustsCreditPerRow() {
        when(scoreMapper.selectById(1)).thenReturn(persisted(1, 85));  // 及格 -> 扣分
        when(scoreMapper.selectById(2)).thenReturn(persisted(2, 40));  // 不及格 -> 不扣

        service.deleteBatch(java.util.Arrays.asList(1, 2));

        verify(scoreMapper).deleteById(1);
        verify(scoreMapper).deleteById(2);
        verify(studentMapper).addScore(STUDENT_ID, -CREDIT);
        verify(studentMapper, never()).addScore(eq(STUDENT_ID), eq(CREDIT));
    }

    // ========== 异常与边界 ==========

    @Test
    @DisplayName("重复录入同一学生同一门课的成绩：抛业务异常")
    void duplicateScoreIsRejected() {
        when(scoreMapper.selectByCourceIdAndStudentId(COURSE_ID, STUDENT_ID)).thenReturn(persisted(9, 80));

        assertThrows(CustomException.class, () -> service.add(score(null, 80, 80)));
        verify(scoreMapper, never()).insert(any());
    }

    @Test
    @DisplayName("课程不存在：抛业务异常而不是 NPE 落 500")
    void missingCourseThrowsBusinessException() {
        when(courseMapper.selectById(COURSE_ID)).thenReturn(null);

        assertThrows(CustomException.class, () -> service.add(score(null, 80, 80)));
    }

    @Test
    @DisplayName("修改不存在的成绩：抛业务异常")
    void updateMissingScoreThrows() {
        when(scoreMapper.selectById(99)).thenReturn(null);

        assertThrows(CustomException.class, () -> service.updateById(score(99, 80, 80)));
    }

    @Test
    @DisplayName("删除不存在的成绩：静默返回，不动学分")
    void deleteMissingScoreIsNoop() {
        when(scoreMapper.selectById(99)).thenReturn(null);

        service.deleteById(99);

        verify(scoreMapper, never()).deleteById(anyInt());
        verify(studentMapper, never()).addScore(anyInt(), anyInt());
    }

    @Test
    @DisplayName("批量删除空集合：不发任何 SQL")
    void deleteBatchWithEmptyListIsNoop() {
        service.deleteBatch(java.util.Collections.emptyList());
        service.deleteBatch(null);

        verify(scoreMapper, never()).deleteById(anyInt());
    }

    // ========== 课程权重与发布状态（课程空间改版） ==========

    @Test
    @DisplayName("总评按课程自己的权重算：平时 40 + 期末 60")
    void addUsesCourseWeights() {
        course.setWeightAttendance(0);
        course.setWeightHomework(0);
        course.setWeightOrdinary(40);
        course.setWeightExam(60);
        Score input = score(null, 60, 90);

        service.add(input);

        // 60*0.4 + 90*0.6 = 24 + 54 = 78
        assertEquals(78.0, input.getScore());
    }

    @Test
    @DisplayName("旧接口录入的成绩直接发布，客户端传来的状态不算数")
    void legacyAddIsAlwaysPublished() {
        Score input = score(null, 80, 80);
        input.setStatus(GradeCalculator.DRAFT);

        service.add(input);

        assertEquals(GradeCalculator.PUBLISHED, input.getStatus());
        verify(studentMapper).addScore(STUDENT_ID, CREDIT);
    }

    @Test
    @DisplayName("课程给考勤、作业设了权重：两项按记录折算后补上，再按四项加权")
    void attendanceAndHomeworkAreFilledFromRecords() {
        course.setWeightAttendance(10);
        course.setWeightHomework(20);
        course.setWeightOrdinary(20);
        course.setWeightExam(50);
        when(gradebookService.livePoints(eq(COURSE_ID), any())).thenReturn(points(100.0, 80.0));
        Score input = score(null, 60, 90);

        service.add(input);

        assertEquals(100.0, input.getAttendanceScore());
        assertEquals(80.0, input.getHomeworkScore());
        // 100*0.1 + 80*0.2 + 60*0.2 + 90*0.5 = 10 + 16 + 12 + 45 = 83
        assertEquals(83.0, input.getScore());
    }

    @Test
    @DisplayName("作业还没有可计入的成绩：在已有的项里按权重归一，旧客户端照样能出总评")
    void missingHomeworkFallsBackToPresentComponents() {
        course.setWeightAttendance(10);
        course.setWeightHomework(20);
        course.setWeightOrdinary(20);
        course.setWeightExam(50);
        when(gradebookService.livePoints(eq(COURSE_ID), any())).thenReturn(points(100.0, null));
        Score input = score(null, 60, 90);

        service.add(input);

        // (100*10 + 60*20 + 90*50) / 80 = 6700 / 80 = 83.75 → 83.8
        assertEquals(83.8, input.getScore());
    }

    @Test
    @DisplayName("成绩超出 0~100：抛业务异常，不落库")
    void outOfRangeComponentIsRejected() {
        assertThrows(CustomException.class, () -> service.add(score(null, 120, 80)));
        verify(scoreMapper, never()).insert(any());
    }

    @Test
    @DisplayName("删除草稿成绩：草稿从没计入学分，删了也不扣")
    void deletingDraftDoesNotRevokeCredit() {
        Score draft = persisted(1, 85);
        draft.setStatus(GradeCalculator.DRAFT);
        when(scoreMapper.selectById(1)).thenReturn(draft);

        service.deleteById(1);

        verify(scoreMapper).deleteById(1);
        verify(studentMapper, never()).addScore(anyInt(), anyInt());
    }

    @Test
    @DisplayName("改草稿成绩：越过及格线也不动学分，且不会顺手把草稿改成已发布")
    void updatingDraftLeavesCreditAndStatusAlone() {
        Score draft = persisted(1, 40);
        draft.setStatus(GradeCalculator.DRAFT);
        when(scoreMapper.selectById(1)).thenReturn(draft);
        Score input = score(1, 90, 90);
        input.setStatus(GradeCalculator.PUBLISHED);

        service.updateById(input);

        assertEquals(null, input.getStatus());
        verify(studentMapper, never()).addScore(anyInt(), anyInt());
    }

    @Test
    @DisplayName("把成绩改到另一个学生名下：原学生扣回学分，新学生加上")
    void movingScoreToAnotherStudentMovesCredit() {
        when(scoreMapper.selectById(1)).thenReturn(persisted(1, 85));
        Score input = score(1, 90, 90);
        input.setStudentId(8);

        service.updateById(input);

        verify(studentMapper).addScore(STUDENT_ID, -CREDIT);
        verify(studentMapper).addScore(8, CREDIT);
    }

    @Test
    @DisplayName("改到的学生这门课已有成绩：抛业务异常")
    void movingOntoExistingScoreIsRejected() {
        when(scoreMapper.selectById(1)).thenReturn(persisted(1, 85));
        when(scoreMapper.selectByCourceIdAndStudentId(COURSE_ID, 8)).thenReturn(persisted(2, 70));
        Score input = score(1, 90, 90);
        input.setStudentId(8);

        assertThrows(CustomException.class, () -> service.updateById(input));
        verify(scoreMapper, never()).updateById(any());
    }

    private static java.util.Map<Integer, Double[]> points(Double attendance, Double homework) {
        java.util.Map<Integer, Double[]> map = new java.util.HashMap<>();
        map.put(STUDENT_ID, new Double[]{attendance, homework});
        return map;
    }
}
