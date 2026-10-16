package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.entity.Notice;
import com.example.exception.CustomException;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.NoticeMapper;
import com.example.support.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 课程空间的门禁：谁算这门课的成员（能进）、谁能「教」（发公告、改简介、布置作业、发起签到……）。
 * 课程空间的各个分页都先过这两道检查，这里覆盖管理员、任课教师、别的老师、选课学生、没选课的学生。
 */
@ExtendWith(MockitoExtension.class)
class CourseSpaceAccessTest {

    /** 数据结构：任课教师 id = 2 */
    private static final int COURSE_ID = 7;
    private static final int TEACHER_ID = 2;

    @Mock
    private CourseMapper courseMapper;
    @Mock
    private ChoiceMapper choiceMapper;
    @Mock
    private NoticeMapper noticeMapper;

    @InjectMocks
    private CourseSpaceService service;

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    private static Course course() {
        Course c = new Course();
        c.setId(COURSE_ID);
        c.setName("数据结构");
        c.setTeacherId(TEACHER_ID);
        return c;
    }

    private static Account account(String role, Integer id) {
        Account a = new Account();
        a.setRole(role);
        a.setId(id);
        return a;
    }

    private void courseExists() {
        when(courseMapper.selectAll(any())).thenReturn(Collections.singletonList(course()));
    }

    private static void assertDenied(Runnable action) {
        CustomException e = assertThrows(CustomException.class, action::run);
        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, e.getCode());
    }

    @Test
    @DisplayName("关系判定：管理员、任课教师、选课学生各归各位；别的老师、没选课的学生、缺 id 的账号都是访客")
    void relationOf() {
        when(choiceMapper.countByStudentAndCourse(3, COURSE_ID)).thenReturn(1);
        when(choiceMapper.countByStudentAndCourse(4, COURSE_ID)).thenReturn(0);
        Course c = course();

        assertEquals(CourseSpaceService.REL_ADMIN, service.relationOf(c, account("ADMIN", 1)));
        assertEquals(CourseSpaceService.REL_TEACHER, service.relationOf(c, account("TEACHER", TEACHER_ID)));
        assertEquals(CourseSpaceService.REL_VISITOR, service.relationOf(c, account("TEACHER", 8)));
        assertEquals(CourseSpaceService.REL_STUDENT, service.relationOf(c, account("STUDENT", 3)));
        assertEquals(CourseSpaceService.REL_VISITOR, service.relationOf(c, account("STUDENT", 4)));
        assertEquals(CourseSpaceService.REL_VISITOR, service.relationOf(c, account("STUDENT", null)));
        assertEquals(CourseSpaceService.REL_VISITOR, service.relationOf(c, account(null, null)));
    }

    @Test
    @DisplayName("学生 id 碰巧等于任课教师 id，也不会被当成老师")
    void studentIdEqualToTeacherIdIsNotTeacher() {
        when(choiceMapper.countByStudentAndCourse(TEACHER_ID, COURSE_ID)).thenReturn(0);

        assertEquals(CourseSpaceService.REL_VISITOR, service.relationOf(course(), account("STUDENT", TEACHER_ID)));
    }

    @Test
    @DisplayName("进课程空间：成员放行并返回身份，访客 403")
    void requireMember() {
        Course c = course();
        CurrentUser.as("STUDENT", 3, "张三");
        when(choiceMapper.countByStudentAndCourse(3, COURSE_ID)).thenReturn(1);
        assertEquals(CourseSpaceService.REL_STUDENT, service.requireMember(c));

        CurrentUser.as("TEACHER", TEACHER_ID, "路易斯");
        assertEquals(CourseSpaceService.REL_TEACHER, service.requireMember(c));

        CurrentUser.as("STUDENT", 4, "李四");
        when(choiceMapper.countByStudentAndCourse(4, COURSE_ID)).thenReturn(0);
        assertDenied(() -> service.requireMember(c));

        CurrentUser.as("TEACHER", 8, "陈敏");
        assertDenied(() -> service.requireMember(c));
    }

    @Test
    @DisplayName("教学操作：只有任课教师和管理员；选了课的学生、别的老师都 403")
    void requireTeaching() {
        Course c = course();
        CurrentUser.as("ADMIN", 1, "管理员");
        service.requireTeaching(c);
        CurrentUser.as("TEACHER", TEACHER_ID, "路易斯");
        service.requireTeaching(c);

        CurrentUser.as("STUDENT", 3, "张三");
        when(choiceMapper.countByStudentAndCourse(3, COURSE_ID)).thenReturn(1);
        assertDenied(() -> service.requireTeaching(c));

        CurrentUser.as("TEACHER", 8, "陈敏");
        assertDenied(() -> service.requireTeaching(c));
    }

    @Test
    @DisplayName("课程 id 缺失报参数缺失，课程不存在报参数错误")
    void requireCourse() {
        CustomException lost = assertThrows(CustomException.class, () -> service.requireCourse(null));
        assertEquals(ResultCodeEnum.PARAM_LOST_ERROR.code, lost.getCode());

        when(courseMapper.selectAll(any())).thenReturn(Collections.emptyList());
        CustomException missing = assertThrows(CustomException.class, () -> service.requireCourse(99));
        assertEquals(ResultCodeEnum.PARAM_ERROR.code, missing.getCode());
    }

    @Test
    @DisplayName("发公告：选课学生 403 且不入库；任课教师发的公告挂在这门课下，标题内容去掉首尾空白")
    void addPost() {
        courseExists();
        Notice form = new Notice();
        form.setTitle("  第 9 周停课  ");
        form.setContent(" 改到第 10 周周四补课 ");

        CurrentUser.as("STUDENT", 3, "张三");
        when(choiceMapper.countByStudentAndCourse(3, COURSE_ID)).thenReturn(1);
        assertDenied(() -> service.addPost(COURSE_ID, form));
        verify(noticeMapper, never()).insert(any());

        CurrentUser.as("TEACHER", TEACHER_ID, "路易斯");
        service.addPost(COURSE_ID, form);
        ArgumentCaptor<Notice> saved = ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).insert(saved.capture());
        assertEquals(COURSE_ID, saved.getValue().getCourseId());
        assertEquals("第 9 周停课", saved.getValue().getTitle());
        assertEquals("改到第 10 周周四补课", saved.getValue().getContent());
        assertEquals("路易斯", saved.getValue().getUser());
    }

    @Test
    @DisplayName("删公告：不能借自己课程的入口删别的课的公告（或全校通知）")
    void deletePostOfAnotherCourse() {
        courseExists();
        CurrentUser.as("TEACHER", TEACHER_ID, "路易斯");
        Notice otherCourse = new Notice();
        otherCourse.setId(30);
        otherCourse.setCourseId(13);
        Notice schoolWide = new Notice();
        schoolWide.setId(31);
        when(noticeMapper.selectById(30)).thenReturn(otherCourse);
        when(noticeMapper.selectById(31)).thenReturn(schoolWide);

        assertThrows(CustomException.class, () -> service.deletePost(COURSE_ID, 30));
        assertThrows(CustomException.class, () -> service.deletePost(COURSE_ID, 31));
        verify(noticeMapper, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("改简介：别的老师 403、不写库；超过 500 字拒绝")
    void updateIntro() {
        courseExists();
        CurrentUser.as("TEACHER", 8, "陈敏");
        assertDenied(() -> service.updateIntro(COURSE_ID, "新简介"));

        CurrentUser.as("TEACHER", TEACHER_ID, "路易斯");
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i <= CourseSpaceService.MAX_INTRO; i++) {
            tooLong.append('字');
        }
        assertThrows(CustomException.class, () -> service.updateIntro(COURSE_ID, tooLong.toString()));
        verify(courseMapper, never()).updateById(any());

        assertEquals("线性表、栈与队列", service.updateIntro(COURSE_ID, " 线性表、栈与队列 "));
        verify(courseMapper).updateById(any());
    }
}
