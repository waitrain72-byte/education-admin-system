package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Choice;
import com.example.entity.Course;
import com.example.entity.Score;
import com.example.exception.CustomException;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.ScoreMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 选课与退选规则：容量（含并发超额后的回归）、时间冲突、重复选课、已结课、退选条件。
 *
 * <p>前身是 ChoiceServiceTest 里的容量与冲突用例：选课规则搬到 {@link EnrollmentService} 后，
 * 旧版选课接口和课程广场共用这一套。</p>
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    private static final int COURSE_ID = 5;
    private static final int STUDENT_ID = 2;

    @Mock
    private ChoiceMapper choiceMapper;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private ScoreMapper scoreMapper;

    @InjectMocks
    private EnrollmentService service;

    private static Course course(int id, String name, String week, String segment, Integer num) {
        Course c = new Course();
        c.setId(id);
        c.setName(name);
        c.setWeek(week);
        c.setSegment(segment);
        c.setNum(num);
        c.setTeacherId(9);
        c.setStatus("已开课");
        return c;
    }

    // ========== 容量 ==========

    @Test
    @DisplayName("未满员：写入选课记录，学生、课程、任课教师都对")
    void notFullAllowsSelection() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID)).thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByCourseId(COURSE_ID)).thenReturn(49);
        when(choiceMapper.selectActiveSlotsByStudentId(STUDENT_ID)).thenReturn(Collections.emptyList());

        service.enroll(STUDENT_ID, COURSE_ID);

        ArgumentCaptor<Choice> saved = ArgumentCaptor.forClass(Choice.class);
        verify(choiceMapper).insert(saved.capture());
        assertEquals(STUDENT_ID, saved.getValue().getStudentId());
        assertEquals(COURSE_ID, saved.getValue().getCourseId());
        assertEquals(9, saved.getValue().getTeacherId());
    }

    @Test
    @DisplayName("人数正好等于上限：拒绝")
    void exactlyFullIsRejected() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID)).thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByCourseId(COURSE_ID)).thenReturn(50);

        assertEquals(ResultCodeEnum.COURSE_NUM_ERROR.code, codeOf(() -> service.enroll(STUDENT_ID, COURSE_ID)));
        verify(choiceMapper, never()).insert(any());
    }

    @Test
    @DisplayName("人数已超上限（历史脏数据 / 并发）：仍然拒绝——回归用例，原先用相等比较会放行")
    void overCapacityIsStillRejected() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID)).thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByCourseId(COURSE_ID)).thenReturn(53);

        assertThrows(CustomException.class, () -> service.enroll(STUDENT_ID, COURSE_ID));
        verify(choiceMapper, never()).insert(any());
    }

    @Test
    @DisplayName("课程没设人数上限：不做容量限制")
    void nullCapacitySkipsCheck() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID)).thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", null));
        when(choiceMapper.selectActiveSlotsByStudentId(STUDENT_ID)).thenReturn(Collections.emptyList());

        service.enroll(STUDENT_ID, COURSE_ID);

        verify(choiceMapper).insert(any());
    }

    // ========== 时间冲突 ==========

    @Test
    @DisplayName("与已选课程同一星期同一大节：拒绝，提示里带冲突课程名")
    void sameSlotIsRejected() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID)).thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByCourseId(COURSE_ID)).thenReturn(1);
        when(choiceMapper.selectActiveSlotsByStudentId(STUDENT_ID))
                .thenReturn(Collections.singletonList(course(6, "中国近代史纲要", "星期五", "第三大节", 50)));

        CustomException ex = assertThrows(CustomException.class, () -> service.enroll(STUDENT_ID, COURSE_ID));
        assertEquals(ResultCodeEnum.SCHEDULE_CONFLICT_ERROR.code, ex.getCode());
        assertTrue(ex.getMsg().contains("中国近代史纲要"), "提示中应包含冲突课程名，实际: " + ex.getMsg());
        verify(choiceMapper, never()).insert(any());
    }

    @Test
    @DisplayName("同一星期但不同大节、同一大节但不同星期：都放行")
    void differentSlotsAreAllowed() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID)).thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByCourseId(COURSE_ID)).thenReturn(1);
        when(choiceMapper.selectActiveSlotsByStudentId(STUDENT_ID)).thenReturn(Arrays.asList(
                course(6, "中国近代史纲要", "星期五", "第一大节", 50),
                course(7, "高等数学", "星期一", "第三大节", 50)));

        service.enroll(STUDENT_ID, COURSE_ID);

        verify(choiceMapper).insert(any());
    }

    @Test
    @DisplayName("多门已选课程里只要有一门冲突就拒绝")
    void conflictAmongSeveralSelectedCourses() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID)).thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByCourseId(COURSE_ID)).thenReturn(1);
        when(choiceMapper.selectActiveSlotsByStudentId(STUDENT_ID)).thenReturn(Arrays.asList(
                course(6, "高等数学", "星期一", "第一大节", 50),
                course(7, "大学英语", "星期五", "第三大节", 50)));

        CustomException ex = assertThrows(CustomException.class, () -> service.enroll(STUDENT_ID, COURSE_ID));
        assertTrue(ex.getMsg().contains("大学英语"));
    }

    @Test
    @DisplayName("没排课（没有星期或大节）的课程不算冲突")
    void unscheduledCourseNeverConflicts() {
        assertNull(EnrollmentService.conflictOf(course(COURSE_ID, "操作系统", null, null, 50),
                Collections.singletonList(course(6, "高等数学", "星期一", "第一大节", 50))));
    }

    // ========== 选课的其他限制 ==========

    @Test
    @DisplayName("已经选过这门课：拒绝（没排课的课程靠时间冲突拦不住重复选）")
    void alreadyEnrolledIsRejected() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID)).thenReturn(course(COURSE_ID, "操作系统", null, null, 50));
        when(choiceMapper.countByStudentAndCourse(STUDENT_ID, COURSE_ID)).thenReturn(1);

        assertEquals(ResultCodeEnum.ALREADY_ENROLLED_ERROR.code, codeOf(() -> service.enroll(STUDENT_ID, COURSE_ID)));
        verify(choiceMapper, never()).insert(any());
    }

    @Test
    @DisplayName("已结课的课不能再选")
    void endedCourseIsRejected() {
        Course ended = course(COURSE_ID, "C 语言程序设计", "星期三", "第二大节", 60);
        ended.setStatus("已结课");
        when(courseMapper.selectByIdForUpdate(COURSE_ID)).thenReturn(ended);

        assertEquals(ResultCodeEnum.COURSE_ENDED_ERROR.code, codeOf(() -> service.enroll(STUDENT_ID, COURSE_ID)));
    }

    @Test
    @DisplayName("课程不存在：抛参数异常而不是 NPE 落 500")
    void missingCourseThrowsParamError() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID)).thenReturn(null);

        assertEquals(ResultCodeEnum.PARAM_ERROR.code, codeOf(() -> service.enroll(STUDENT_ID, COURSE_ID)));
    }

    // ========== 退选 ==========

    @Test
    @DisplayName("退选：删除选课记录，成绩册里的草稿一并清掉")
    void dropRemovesChoiceAndDraftScore() {
        when(courseMapper.selectById(COURSE_ID)).thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByStudentAndCourse(STUDENT_ID, COURSE_ID)).thenReturn(1);
        Score draft = new Score();
        draft.setId(77);
        draft.setStatus(GradeCalculator.DRAFT);
        when(scoreMapper.selectByCourceIdAndStudentId(COURSE_ID, STUDENT_ID)).thenReturn(draft);

        service.drop(STUDENT_ID, COURSE_ID);

        verify(scoreMapper).deleteById(77);
        verify(choiceMapper).deleteByStudentAndCourse(STUDENT_ID, COURSE_ID);
    }

    @Test
    @DisplayName("成绩已发布或课程已结课：不能退选")
    void dropIsLockedAfterPublishingOrEnding() {
        when(courseMapper.selectById(COURSE_ID)).thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByStudentAndCourse(STUDENT_ID, COURSE_ID)).thenReturn(1);
        Score published = new Score();
        published.setStatus(GradeCalculator.PUBLISHED);
        when(scoreMapper.selectByCourceIdAndStudentId(COURSE_ID, STUDENT_ID)).thenReturn(published);

        assertEquals(ResultCodeEnum.DROP_LOCKED_ERROR.code, codeOf(() -> service.drop(STUDENT_ID, COURSE_ID)));
        verify(choiceMapper, never()).deleteByStudentAndCourse(anyInt(), anyInt());

        Course ended = course(COURSE_ID, "线性代数", "星期五", "第三大节", 50);
        ended.setStatus("已结课");
        assertFalse(service.canDrop(STUDENT_ID, ended));
    }

    @Test
    @DisplayName("没选这门课：退选报参数错误")
    void dropWithoutEnrollmentIsRejected() {
        when(courseMapper.selectById(COURSE_ID)).thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByStudentAndCourse(STUDENT_ID, COURSE_ID)).thenReturn(0);

        assertEquals(ResultCodeEnum.PARAM_ERROR.code, codeOf(() -> service.drop(STUDENT_ID, COURSE_ID)));
    }

    private static String codeOf(Runnable action) {
        CustomException e = assertThrows(CustomException.class, action::run);
        return e.getCode();
    }
}
