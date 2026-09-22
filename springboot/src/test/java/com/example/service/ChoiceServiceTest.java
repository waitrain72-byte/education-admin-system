package com.example.service;

import com.example.entity.Choice;
import com.example.entity.Course;
import com.example.exception.CustomException;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 选课业务单元测试：容量校验与上课时间冲突判断。
 *
 * <p>覆盖两处曾经的缺陷：</p>
 * <ul>
 *   <li>满员判断用 {@code num.equals(size)} 相等比较，一旦并发导致人数超过 num 就再也拦不住；</li>
 *   <li>冲突判断在循环里逐条查课程（N+1），现由一条 join 查询取回未结课课程的时段。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ChoiceServiceTest {

    private static final int COURSE_ID = 5;
    private static final int STUDENT_ID = 2;

    @Mock
    private ChoiceMapper choiceMapper;
    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private ChoiceService service;

    private Course course(int id, String name, String week, String segment, int num) {
        Course c = new Course();
        c.setId(id);
        c.setName(name);
        c.setWeek(week);
        c.setSegment(segment);
        c.setNum(num);
        return c;
    }

    private Choice choice() {
        Choice ch = new Choice();
        ch.setStudentId(STUDENT_ID);
        ch.setCourseId(COURSE_ID);
        ch.setTeacherId(1);
        return ch;
    }

    // ========== 容量校验 ==========

    @Test
    @DisplayName("未满员：正常写入选课记录")
    void notFullAllowsSelection() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID))
                .thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByCourseId(COURSE_ID)).thenReturn(49);
        when(choiceMapper.selectActiveSlotsByStudentId(STUDENT_ID)).thenReturn(Collections.emptyList());

        Choice ch = choice();
        service.add(ch);

        verify(choiceMapper).insert(ch);
    }

    @Test
    @DisplayName("人数正好等于上限：拒绝选课")
    void exactlyFullIsRejected() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID))
                .thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByCourseId(COURSE_ID)).thenReturn(50);

        CustomException ex = assertThrows(CustomException.class, () -> service.add(choice()));
        assertEquals("5006", ex.getCode());
        verify(choiceMapper, never()).insert(any());
    }

    @Test
    @DisplayName("人数已超上限（历史脏数据/并发）：仍然拒绝——回归用例，原先用相等比较会放行")
    void overCapacityIsStillRejected() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID))
                .thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByCourseId(COURSE_ID)).thenReturn(53);

        assertThrows(CustomException.class, () -> service.add(choice()));
        verify(choiceMapper, never()).insert(any());
    }

    @Test
    @DisplayName("课程未设置人数上限：不做容量限制")
    void nullCapacitySkipsCheck() {
        Course c = course(COURSE_ID, "线性代数", "星期五", "第三大节", 0);
        c.setNum(null);
        when(courseMapper.selectByIdForUpdate(COURSE_ID)).thenReturn(c);
        when(choiceMapper.selectActiveSlotsByStudentId(STUDENT_ID)).thenReturn(Collections.emptyList());

        service.add(choice());

        verify(choiceMapper).insert(any());
    }

    // ========== 时间冲突 ==========

    @Test
    @DisplayName("与已选课程同一星期同一大节：拒绝并提示冲突课程名")
    void sameSlotIsRejected() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID))
                .thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByCourseId(COURSE_ID)).thenReturn(1);
        when(choiceMapper.selectActiveSlotsByStudentId(STUDENT_ID))
                .thenReturn(Collections.singletonList(course(6, "中国近代史纲要", "星期五", "第三大节", 50)));

        CustomException ex = assertThrows(CustomException.class, () -> service.add(choice()));
        assertTrue(ex.getMsg().contains("中国近代史纲要"), "提示中应包含冲突课程名，实际: " + ex.getMsg());
        verify(choiceMapper, never()).insert(any());
    }

    @Test
    @DisplayName("同一星期但不同大节：放行")
    void sameWeekDifferentSegmentIsAllowed() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID))
                .thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByCourseId(COURSE_ID)).thenReturn(1);
        when(choiceMapper.selectActiveSlotsByStudentId(STUDENT_ID))
                .thenReturn(Collections.singletonList(course(6, "中国近代史纲要", "星期五", "第一大节", 50)));

        service.add(choice());

        verify(choiceMapper).insert(any());
    }

    @Test
    @DisplayName("同一大节但不同星期：放行")
    void sameSegmentDifferentWeekIsAllowed() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID))
                .thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByCourseId(COURSE_ID)).thenReturn(1);
        when(choiceMapper.selectActiveSlotsByStudentId(STUDENT_ID))
                .thenReturn(Collections.singletonList(course(6, "高等数学", "星期一", "第三大节", 50)));

        service.add(choice());

        verify(choiceMapper).insert(any());
    }

    @Test
    @DisplayName("多门已选课程中只要有一门冲突就拒绝")
    void conflictAmongSeveralSelectedCourses() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID))
                .thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByCourseId(COURSE_ID)).thenReturn(1);
        when(choiceMapper.selectActiveSlotsByStudentId(STUDENT_ID)).thenReturn(Arrays.asList(
                course(6, "高等数学", "星期一", "第一大节", 50),
                course(7, "大学英语", "星期五", "第三大节", 50)
        ));

        CustomException ex = assertThrows(CustomException.class, () -> service.add(choice()));
        assertTrue(ex.getMsg().contains("大学英语"));
    }

    @Test
    @DisplayName("已结课的课程不占用时段——由 SQL 过滤，冲突判断拿不到这类记录")
    void finishedCoursesDoNotBlock() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID))
                .thenReturn(course(COURSE_ID, "线性代数", "星期五", "第三大节", 50));
        when(choiceMapper.countByCourseId(COURSE_ID)).thenReturn(1);
        // selectActiveSlotsByStudentId 的 SQL 带 status <> '已结课'，已结课课程不会出现在结果里
        when(choiceMapper.selectActiveSlotsByStudentId(STUDENT_ID)).thenReturn(Collections.emptyList());

        service.add(choice());

        verify(choiceMapper).insert(any());
    }

    // ========== 异常 ==========

    @Test
    @DisplayName("课程不存在：抛参数异常而不是 NPE 落 500")
    void missingCourseThrowsParamError() {
        when(courseMapper.selectByIdForUpdate(COURSE_ID)).thenReturn(null);

        CustomException ex = assertThrows(CustomException.class, () -> service.add(choice()));
        assertEquals("400", ex.getCode());
        verify(choiceMapper, never()).insert(any());
    }
}
