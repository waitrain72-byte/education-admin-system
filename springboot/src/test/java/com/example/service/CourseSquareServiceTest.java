package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Course;
import com.example.exception.CustomException;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.ScoreMapper;
import com.example.mapper.WorkbenchMapper;
import com.example.support.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * 课程广场：筛选、名额、已选与时间冲突标记、推荐；选课只给学生。
 */
@ExtendWith(MockitoExtension.class)
class CourseSquareServiceTest {

    @Mock
    private CourseMapper courseMapper;
    @Mock
    private ChoiceMapper choiceMapper;
    @Mock
    private WorkbenchMapper workbenchMapper;
    @Mock
    private RecommendService recommendService;
    @Mock
    private EnrollmentService enrollmentService;
    @Mock
    private ScoreMapper scoreMapper;

    @InjectMocks
    private CourseSquareService service;

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    @Test
    @DisplayName("学生看到的广场：默认不含已结课；标出已选、名额、和已选课程的冲突、推荐理由")
    @SuppressWarnings("unchecked")
    void studentSquare() {
        CurrentUser.as("STUDENT", 1, "张三");
        Course math = course(1, "高等数学", "必修", "星期一", "第一大节", "已开课", 120);
        Course english = course(10, "大学英语", "必修", "星期一", "第一大节", "已开课", 2);
        Course python = course(12, "Python 数据分析", "选修", "星期四", "第五大节", "未开课", 40);
        Course c = course(14, "C 语言程序设计", "必修", "星期三", "第二大节", "已结课", 60);
        when(courseMapper.selectAll(any())).thenReturn(Arrays.asList(math, english, python, c));
        when(workbenchMapper.countStudentsAll()).thenReturn(Arrays.asList(count(1, 15), count(10, 2), count(12, 5)));
        when(choiceMapper.selectCourseIdsByStudentId(1)).thenReturn(Collections.singletonList(1));
        when(choiceMapper.selectActiveSlotsByStudentId(1)).thenReturn(Collections.singletonList(math));
        Map<String, Object> rec = new HashMap<>();
        rec.put("id", 12);
        rec.put("reason", "与已选《高等数学》相似度 40%");
        when(recommendService.recommendForStudent(1, CourseSquareService.RECOMMEND_LIMIT)).thenReturn(Collections.singletonList(rec));

        Map<String, Object> data = service.square(new CourseSquareService.Query());

        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("courses");
        assertEquals(3, rows.size());
        Map<String, Object> mathRow = find(rows, 1);
        assertEquals(true, mathRow.get("enrolled"));
        assertEquals(true, mathRow.get("canDrop"));
        assertEquals(105, mathRow.get("seatsLeft"));
        Map<String, Object> englishRow = find(rows, 10);
        assertEquals("高等数学", englishRow.get("conflict"));
        assertEquals(0, englishRow.get("seatsLeft"));
        assertEquals("与已选《高等数学》相似度 40%", find(rows, 12).get("reason"));
        assertEquals(1, ((List<?>) data.get("recommendations")).size());
        assertEquals(true, data.get("canEnroll"));
    }

    @Test
    @DisplayName("只看有名额的：满员的课不出现")
    @SuppressWarnings("unchecked")
    void availableFilterHidesFullCourses() {
        CurrentUser.as("TEACHER", 2, "路易斯");
        when(courseMapper.selectAll(any())).thenReturn(Arrays.asList(
                course(1, "高等数学", "必修", "星期一", "第一大节", "已开课", 120),
                course(10, "大学英语", "必修", "星期一", "第三大节", "已开课", 2)));
        when(workbenchMapper.countStudentsAll()).thenReturn(Arrays.asList(count(1, 15), count(10, 2)));
        CourseSquareService.Query q = new CourseSquareService.Query();
        q.available = true;

        Map<String, Object> data = service.square(q);

        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("courses");
        assertEquals(1, rows.size());
        assertEquals(false, data.get("canEnroll"));
        assertFalse(rows.get(0).containsKey("enrolled"));
        verify(recommendService, never()).recommendForStudent(anyInt(), anyInt());
    }

    @Test
    @DisplayName("筛选：关键字匹配课程名、教师、教室（不分大小写）；类型、星期、状态精确匹配")
    void matchesFilters() {
        Course java = course(8, "Java 程序设计", "必修", "星期二", "第三大节", "已开课", 40);
        java.setTeacherName("路易斯");
        java.setRoom("7711");
        CourseSquareService.Query q = new CourseSquareService.Query();
        q.keyword = "java";
        assertTrue(CourseSquareService.matches(java, q));
        q.keyword = "路易";
        assertTrue(CourseSquareService.matches(java, q));
        q.keyword = "7711";
        assertTrue(CourseSquareService.matches(java, q));
        q.keyword = "python";
        assertFalse(CourseSquareService.matches(java, q));

        CourseSquareService.Query byType = new CourseSquareService.Query();
        byType.type = "选修";
        assertFalse(CourseSquareService.matches(java, byType));
        CourseSquareService.Query byWeek = new CourseSquareService.Query();
        byWeek.week = "星期二";
        assertTrue(CourseSquareService.matches(java, byWeek));

        Course ended = course(14, "C 语言程序设计", "必修", "星期三", "第二大节", "已结课", 60);
        assertFalse(CourseSquareService.matches(ended, new CourseSquareService.Query()));
        CourseSquareService.Query withEnded = new CourseSquareService.Query();
        withEnded.includeEnded = true;
        assertTrue(CourseSquareService.matches(ended, withEnded));
    }

    @Test
    @DisplayName("只有学生能在广场选课、退选")
    void onlyStudentsEnroll() {
        CurrentUser.as("TEACHER", 2, "路易斯");

        CustomException e = assertThrows(CustomException.class, () -> service.enroll(8));
        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, e.getCode());
        assertThrows(CustomException.class, () -> service.drop(8));
        verify(enrollmentService, never()).enroll(anyInt(), anyInt());
    }

    @Test
    @DisplayName("学生选课：交给 EnrollmentService，返回课程卡片")
    void studentEnrollDelegates() {
        CurrentUser.as("STUDENT", 1, "张三");
        when(enrollmentService.enroll(1, 12)).thenReturn(course(12, "Python 数据分析", "选修", "星期四", "第五大节", "未开课", 40));
        when(choiceMapper.countByCourseId(12)).thenReturn(6);

        Map<String, Object> card = service.enroll(12);

        assertEquals("Python 数据分析", card.get("name"));
        assertEquals(6L, card.get("studentCount"));
        assertNull(card.get("seatsLeft"));
    }

    private static Course course(int id, String name, String type, String week, String segment, String status, Integer num) {
        Course c = new Course();
        c.setId(id);
        c.setName(name);
        c.setType(type);
        c.setWeek(week);
        c.setSegment(segment);
        c.setStatus(status);
        c.setNum(num);
        return c;
    }

    private static Map<String, Object> count(int courseId, long total) {
        Map<String, Object> row = new HashMap<>();
        row.put("courseId", courseId);
        row.put("total", total);
        return row;
    }

    private static Map<String, Object> find(List<Map<String, Object>> rows, int id) {
        return rows.stream().filter(r -> Integer.valueOf(id).equals(r.get("id"))).findFirst().orElseThrow(IllegalStateException::new);
    }
}
