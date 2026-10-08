package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Score;
import com.example.entity.Student;
import com.example.exception.CustomException;
import com.example.mapper.ScoreMapper;
import com.example.mapper.StudentMapper;
import com.example.mapper.WorkbenchMapper;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成绩单：只列已发布的成绩，没发布的显示未出分；同一门课两行成绩取最新的；加权平均按学分算。
 */
@ExtendWith(MockitoExtension.class)
class TranscriptServiceTest {

    @Mock
    private ScoreMapper scoreMapper;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private WorkbenchMapper workbenchMapper;

    @InjectMocks
    private TranscriptService service;

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    @Test
    @DisplayName("老师、管理员没有成绩单")
    void onlyStudents() {
        CurrentUser.as("TEACHER", 2, "路易斯");

        CustomException e = assertThrows(CustomException.class, () -> service.mine());
        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, e.getCode());
        verify(scoreMapper, never()).selectAll(any());
    }

    @Test
    @DisplayName("出了分的在前；没发布的显示未出分；加权平均按学分；同一门课取最新一行")
    void buildsTranscript() {
        CurrentUser.as("STUDENT", 1, "张三");
        when(scoreMapper.selectAll(any())).thenReturn(Arrays.asList(
                score(10, 6, 55.0),   // 旧的一行
                score(11, 6, 72.0),   // 同一门课较新的一行
                score(12, 15, 91.0)));
        when(scoreMapper.selectTranscriptCourses(1)).thenReturn(Arrays.asList(
                course(1, "高等数学", 5),
                course(6, "中国近代史纲要", 2),
                course(15, "大学物理", 4)));
        Student me = new Student();
        me.setScore(6);
        when(studentMapper.selectById(1)).thenReturn(me);
        when(workbenchMapper.requiredCredits(1)).thenReturn(50);
        when(workbenchMapper.scoresWithCredit(1)).thenReturn(Collections.emptyList());

        Map<String, Object> data = service.mine();

        ArgumentCaptor<Score> probe = ArgumentCaptor.forClass(Score.class);
        verify(scoreMapper).selectAll(probe.capture());
        assertEquals(1, probe.getValue().getStudentId());
        assertEquals(GradeCalculator.PUBLISHED, probe.getValue().getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("rows");
        assertEquals(Arrays.asList(15, 6, 1), Arrays.asList(rows.get(0).get("courseId"), rows.get(1).get("courseId"), rows.get(2).get("courseId")));
        assertEquals(72.0, rows.get(1).get("total"));
        assertEquals(true, rows.get(1).get("passed"));
        assertEquals(2.2, rows.get(1).get("gradePoint"));
        assertEquals(false, rows.get(2).get("published"));
        assertNull(rows.get(2).get("total"));

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) data.get("summary");
        assertEquals(6, summary.get("credits"));
        assertEquals(50, summary.get("requiredCredits"));
        // (91×4 + 72×2) ÷ 6 = 84.67 → 84.7
        assertEquals(84.7, summary.get("average"));
        assertEquals(2, summary.get("passed"));
        assertEquals(0, summary.get("failed"));
        assertEquals(1, summary.get("pending"));
    }

    private static Score score(int id, int courseId, double total) {
        Score s = new Score();
        s.setId(id);
        s.setCourseId(courseId);
        s.setStudentId(1);
        s.setScore(total);
        s.setStatus(GradeCalculator.PUBLISHED);
        return s;
    }

    private static Map<String, Object> course(int id, String name, int credit) {
        Map<String, Object> c = new HashMap<>();
        c.put("courseId", id);
        c.put("courseName", name);
        c.put("credit", credit);
        return c;
    }
}
