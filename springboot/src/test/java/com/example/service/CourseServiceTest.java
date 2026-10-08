package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Course;
import com.example.exception.CustomException;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import com.example.support.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 课程管理：删除前检查有没有人选、表单不能改总评权重、教师只能调排课。
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseMapper courseMapper;
    @Mock
    private ChoiceMapper choiceMapper;

    @InjectMocks
    private CourseService service;

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    @Test
    @DisplayName("已经有学生选的课不能删；没人选的可以")
    void courseWithStudentsCannotBeDeleted() {
        CurrentUser.as("ADMIN", 1, "管理员");
        when(choiceMapper.countByCourseId(7)).thenReturn(15);
        when(choiceMapper.countByCourseId(13)).thenReturn(0);

        CustomException e = assertThrows(CustomException.class, () -> service.deleteById(7));
        assertEquals(ResultCodeEnum.COURSE_IN_USE_ERROR.code, e.getCode());
        service.deleteById(13);

        verify(courseMapper, never()).deleteById(7);
        verify(courseMapper).deleteById(13);
    }

    @Test
    @DisplayName("批量删除逐条检查：有一门有人选就整体拒绝")
    void batchDeleteChecksEachCourse() {
        CurrentUser.as("ADMIN", 1, "管理员");
        when(choiceMapper.countByCourseId(13)).thenReturn(0);
        when(choiceMapper.countByCourseId(7)).thenReturn(15);

        assertThrows(CustomException.class, () -> service.deleteBatch(Arrays.asList(13, 7)));
        verify(courseMapper, never()).deleteBatchIds(any());
    }

    @Test
    @DisplayName("老师不能删课")
    void teacherCannotDelete() {
        CurrentUser.as("TEACHER", 2, "路易斯");

        assertThrows(CustomException.class, () -> service.deleteById(13));
        verify(courseMapper, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("管理员改课程：总评权重只能在成绩册里改，表单带上来的一律忽略")
    void adminUpdateStripsWeights() {
        CurrentUser.as("ADMIN", 1, "管理员");
        when(courseMapper.selectById(8)).thenReturn(course(8, 2, "7711", "已开课"));
        Course form = new Course();
        form.setId(8);
        form.setName("Java 程序设计");
        form.setWeightExam(100);
        form.setWeightOrdinary(0);

        service.updateById(form);

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseMapper).updateById(saved.capture());
        assertNull(saved.getValue().getWeightExam());
        assertNull(saved.getValue().getWeightOrdinary());
        assertEquals("Java 程序设计", saved.getValue().getName());
    }

    @Test
    @DisplayName("老师改自己的课：只能动教室、星期、大节、状态")
    void teacherCanOnlyReschedule() {
        CurrentUser.as("TEACHER", 2, "路易斯");
        Course existing = new Course();
        existing.setId(8);
        existing.setTeacherId(2);
        existing.setName("Java 程序设计");
        existing.setRoom("7711");
        when(courseMapper.selectById(8)).thenReturn(existing);
        Course form = new Course();
        form.setId(8);
        form.setName("改个名字");
        form.setRoom("7706");
        form.setNum(999);

        service.updateById(form);

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseMapper).updateById(saved.capture());
        assertEquals("7706", saved.getValue().getRoom());
        assertNull(saved.getValue().getName());
        assertNull(saved.getValue().getNum());
    }

    @Test
    @DisplayName("换任课教师：选课记录上的教师一起改；没换就不动选课记录")
    void changingTeacherUpdatesEnrolments() {
        CurrentUser.as("ADMIN", 1, "管理员");
        when(courseMapper.selectById(8)).thenReturn(course(8, 2, null, "已开课"));
        Course form = new Course();
        form.setId(8);
        form.setTeacherId(5);

        service.updateById(form);
        verify(choiceMapper).updateTeacherOfCourse(8, 5);

        Course same = new Course();
        same.setId(8);
        same.setTeacherId(2);
        service.updateById(same);
        verify(choiceMapper, never()).updateTeacherOfCourse(8, 2);
    }

    @Test
    @DisplayName("把已结课的课重新开起来：原来的时段已经排给别的课时不允许")
    void reopeningChecksTheOldSlot() {
        CurrentUser.as("ADMIN", 1, "管理员");
        Course finished = course(8, 2, "7711", "已结课");
        finished.setWeek("周一");
        finished.setSegment("第一大节");
        when(courseMapper.selectById(8)).thenReturn(finished);
        Course other = course(9, 3, "7711", "已开课");
        other.setName("数据结构");
        when(courseMapper.selectRoomOccupied(any())).thenReturn(other);
        Course form = new Course();
        form.setId(8);
        form.setStatus("已开课");

        CustomException e = assertThrows(CustomException.class, () -> service.updateById(form));
        assertEquals(ResultCodeEnum.ROOM_OCCUPIED_ERROR.code, e.getCode());
        verify(courseMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("改成已结课的课不再占教室，不查占用")
    void finishingSkipsTheRoomCheck() {
        CurrentUser.as("ADMIN", 1, "管理员");
        Course active = course(8, 2, "7711", "已开课");
        active.setWeek("周一");
        active.setSegment("第一大节");
        when(courseMapper.selectById(8)).thenReturn(active);
        Course form = new Course();
        form.setId(8);
        form.setStatus("已结课");

        service.updateById(form);

        verify(courseMapper, never()).selectRoomOccupied(any());
        verify(courseMapper).updateById(form);
    }

    private static Course course(Integer id, Integer teacherId, String room, String status) {
        Course c = new Course();
        c.setId(id);
        c.setTeacherId(teacherId);
        c.setRoom(room);
        c.setStatus(status);
        return c;
    }
}
