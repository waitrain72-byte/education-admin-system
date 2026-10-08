package com.example.mapper;

import com.example.entity.Course;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 首页（工作台）聚合查询：只读，全部 #{} 预编译
 */
public interface WorkbenchMapper {

    /** 教师名下各课程的选课人数 */
    @Select("select ch.course_id as courseId, count(*) as total from choice ch"
            + " join course c on ch.course_id = c.id"
            + " where c.teacher_id = #{teacherId} group by ch.course_id")
    List<Map<String, Object>> countStudentsByTeacher(Integer teacherId);

    /** 全部课程的选课人数（管理员的课程列表用） */
    @Select("select course_id as courseId, count(*) as total from choice group by course_id")
    List<Map<String, Object>> countStudentsAll();

    /** 教师名下各课程待批改的作业（已提交、还没打分） */
    @Select("select course_id as courseId, count(*) as total from homework"
            + " where teacher_id = #{teacherId} and score is null group by course_id")
    List<Map<String, Object>> countUngradedByTeacher(Integer teacherId);

    /** 学生已发布、有总评的成绩，带课程学分（算绩点用；成绩册里的草稿不算） */
    @Select("select s.course_id as courseId, s.score as score, c.score as credit"
            + " from score s join course c on s.course_id = c.id"
            + " where s.student_id = #{studentId} and s.score is not null and s.status = '已发布'")
    List<Map<String, Object>> scoresWithCredit(Integer studentId);

    /** 学生还没交、也没过截止时间的作业（首页待办），截止早的在前；now 为 yyyy-MM-dd HH:mm */
    @Select("select a.id, a.course_id as courseId, c.name as courseName, a.title, a.deadline from assignment a"
            + " join choice ch on ch.course_id = a.course_id and ch.student_id = #{studentId}"
            + " join course c on c.id = a.course_id"
            + " where a.deadline >= #{now}"
            + " and not exists (select 1 from homework h where h.assignment_id = a.id and h.student_id = #{studentId})"
            + " order by a.deadline, a.id limit 5")
    List<Map<String, Object>> pendingAssignments(@Param("studentId") Integer studentId, @Param("now") String now);

    /** 学生所在专业要求修满的学分（未分配专业时为 null） */
    @Select("select sp.score from student st join speciality sp on st.speciality_id = sp.id where st.id = #{studentId}")
    Integer requiredCredits(Integer studentId);

    /** 正在上课的课程数 */
    @Select("select count(*) from course where status = '已开课'")
    int countActiveCourses();

    /** 还没排好教室或上课时间、也没结课的课程（管理员首页提醒补排） */
    @Select("select c.id, c.name, c.status, t.name as teacherName from course c"
            + " left join teacher t on c.teacher_id = t.id"
            + " where ifnull(c.status, '') <> '已结课'"
            + " and (ifnull(c.room, '') = '' or ifnull(c.week, '') = '' or ifnull(c.segment, '') = '')"
            + " order by c.id desc limit 5")
    List<Course> unscheduledCourses();
}
