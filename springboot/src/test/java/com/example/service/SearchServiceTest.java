package com.example.service;

import com.example.entity.Course;
import com.example.entity.Notice;
import com.example.mapper.CourseMapper;
import com.example.mapper.NoticeMapper;
import com.example.mapper.SearchMapper;
import com.example.support.CurrentUser;
import com.github.pagehelper.PageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 全局搜索：关键字规整、按角色收窄范围、人员名单只对管理员开放
 */
class SearchServiceTest {

    private final CourseMapper courseMapper = mock(CourseMapper.class);
    private final NoticeMapper noticeMapper = mock(NoticeMapper.class);
    private final SearchMapper searchMapper = mock(SearchMapper.class);
    private final SearchService service = new SearchService();

    {
        ReflectionTestUtils.setField(service, "courseMapper", courseMapper);
        ReflectionTestUtils.setField(service, "noticeMapper", noticeMapper);
        ReflectionTestUtils.setField(service, "searchMapper", searchMapper);
    }

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
        // Mapper 是 mock，不会真的执行查询，PageHelper 的线程变量不会被自动清掉，手动清理免得串到别的测试
        PageHelper.clearPage();
    }

    @Test
    @DisplayName("关键字：去首尾空白；空串、超过 30 字视为无结果且不查库")
    void normalize() {
        assertEquals("高数", SearchService.normalize("  高数 "));
        assertNull(SearchService.normalize("   "));
        assertNull(SearchService.normalize(null));
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i < 31; i++) {
            tooLong.append('a');
        }
        assertNull(SearchService.normalize(tooLong.toString()));

        Map<String, Object> result = service.search("   ");
        assertTrue(((List<?>) result.get("courses")).isEmpty());
        verify(courseMapper, never()).selectAll(any());
    }

    @Test
    @DisplayName("教师只搜得到自己开的课，看不到学生和教师名单")
    void teacherScope() {
        CurrentUser.as("TEACHER", 2, "路易斯");
        service.search("数据");
        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseMapper).selectAll(captor.capture());
        assertEquals(2, captor.getValue().getTeacherId());
        assertEquals("数据", captor.getValue().getName());
        verify(searchMapper, never()).searchStudents(anyString());
        verify(searchMapper, never()).searchTeachers(anyString());
        verify(noticeMapper).selectAll(any(Notice.class));
    }

    @Test
    @DisplayName("管理员额外搜学生与教师；学生搜课程不限任课教师")
    void adminAndStudentScope() {
        CurrentUser.as("ADMIN", 1, "管理员");
        service.search("张");
        verify(searchMapper).searchStudents("张");
        verify(searchMapper).searchTeachers("张");

        CurrentUser.as("STUDENT", 3, "张三");
        service.search("Java");
        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseMapper, org.mockito.Mockito.times(2)).selectAll(captor.capture());
        assertNull(captor.getValue().getTeacherId());
    }
}
