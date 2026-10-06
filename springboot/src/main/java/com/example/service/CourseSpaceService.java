package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.entity.Teacher;
import com.example.exception.CustomException;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.TeacherMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程空间：以「一门课」为单位的数据入口。
 *
 * <p>当前登录人与这门课的关系决定能看到什么：</p>
 * <ul>
 *   <li>admin —— 管理员，全部可见；</li>
 *   <li>teacher —— 这门课的任课教师；</li>
 *   <li>student —— 选了这门课的学生；</li>
 *   <li>visitor —— 其他人（如还没选课的学生在课程广场里预览），只能看课程基本信息。</li>
 * </ul>
 */
@Service
public class CourseSpaceService {

    public static final String REL_ADMIN = "admin";
    public static final String REL_TEACHER = "teacher";
    public static final String REL_STUDENT = "student";
    public static final String REL_VISITOR = "visitor";

    @Resource
    private CourseMapper courseMapper;
    @Resource
    private ChoiceMapper choiceMapper;
    @Resource
    private TeacherMapper teacherMapper;

    /** 课程（带任课教师姓名）；不存在时报参数错误 */
    public Course requireCourse(Integer courseId) {
        if (courseId == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        Course probe = new Course();
        probe.setId(courseId);
        List<Course> list = courseMapper.selectAll(probe);
        if (list.isEmpty()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        return list.get(0);
    }

    /** 当前登录人与课程的关系 */
    public String relationOf(Course course) {
        Account current = TokenUtils.getCurrentUser();
        return relationOf(course, current);
    }

    String relationOf(Course course, Account current) {
        String role = current.getRole();
        if (RoleEnum.ADMIN.name().equals(role)) {
            return REL_ADMIN;
        }
        if (RoleEnum.TEACHER.name().equals(role) && current.getId() != null
                && current.getId().equals(course.getTeacherId())) {
            return REL_TEACHER;
        }
        if (RoleEnum.STUDENT.name().equals(role) && current.getId() != null
                && choiceMapper.countByStudentAndCourse(current.getId(), course.getId()) > 0) {
            return REL_STUDENT;
        }
        return REL_VISITOR;
    }

    /** 课程成员才能进入（管理员、任课教师、选课学生），否则 403 */
    public String requireMember(Course course) {
        String relation = relationOf(course);
        if (REL_VISITOR.equals(relation)) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        return relation;
    }

    /** 只有任课教师和管理员能做（布置作业、发起签到、录成绩……） */
    public void requireTeaching(Course course) {
        String relation = relationOf(course);
        if (!REL_TEACHER.equals(relation) && !REL_ADMIN.equals(relation)) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
    }

    /** 课程概览：课程信息、任课教师、选课人数、当前登录人与课程的关系 */
    public Map<String, Object> overview(Integer courseId) {
        Course course = requireCourse(courseId);
        String relation = relationOf(course);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("course", WorkbenchService.courseCard(course.getId(), course.getName(), course.getType(),
                course.getScore(), course.getTeacherName(), course.getRoom(), course.getWeek(), course.getSegment(),
                course.getStatus(), course.getNum()));
        data.put("teacher", teacherBrief(course.getTeacherId()));
        data.put("studentCount", choiceMapper.countByCourseId(courseId));
        data.put("relation", relation);
        return data;
    }

    /** 任课教师的公开信息：姓名、职称、头像、邮箱（不含电话等私人信息） */
    private Map<String, Object> teacherBrief(Integer teacherId) {
        if (teacherId == null) {
            return null;
        }
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            return null;
        }
        Map<String, Object> brief = new LinkedHashMap<>();
        brief.put("id", teacher.getId());
        brief.put("name", teacher.getName());
        brief.put("title", teacher.getTitle());
        brief.put("avatar", teacher.getAvatar());
        brief.put("email", teacher.getEmail());
        return brief;
    }
}
