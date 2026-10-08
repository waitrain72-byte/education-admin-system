package com.example.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 教务后台「人员」页的列表查询：学生、教师、管理员三类账号，按姓名或账号模糊搜索。只读、不含密码。
 */
public interface PeopleMapper {

    @Select("<script>select s.id, s.username, s.name, s.avatar, s.score as credits,"
            + " s.college_id as collegeId, s.speciality_id as specialityId, s.class_id as classId,"
            + " co.name as collegeName, sp.name as specialityName, cl.name as className"
            + " from student s"
            + " left join college co on s.college_id = co.id"
            + " left join speciality sp on s.speciality_id = sp.id"
            + " left join classes cl on s.class_id = cl.id"
            + " <where>"
            + " <if test='keyword != null'> and (s.name like concat('%', #{keyword}, '%') or s.username like concat('%', #{keyword}, '%'))</if>"
            + " <if test='collegeId != null'> and s.college_id = #{collegeId}</if>"
            + " <if test='specialityId != null'> and s.speciality_id = #{specialityId}</if>"
            + " <if test='classId != null'> and s.class_id = #{classId}</if>"
            + " </where> order by s.id desc</script>")
    List<Map<String, Object>> students(@Param("keyword") String keyword, @Param("collegeId") Integer collegeId,
                                       @Param("specialityId") Integer specialityId, @Param("classId") Integer classId);

    /** 教师：带正在上的课数、担任班主任的班数 */
    @Select("<script>select t.id, t.username, t.name, t.avatar, t.phone, t.email, t.title,"
            + " (select count(*) from course c where c.teacher_id = t.id and ifnull(c.status, '') &lt;&gt; '已结课') as activeCourses,"
            + " (select count(*) from classes cl where cl.teacher_id = t.id) as headOf"
            + " from teacher t"
            + " <where>"
            + " <if test='keyword != null'> and (t.name like concat('%', #{keyword}, '%') or t.username like concat('%', #{keyword}, '%')"
            + " or t.title like concat('%', #{keyword}, '%'))</if>"
            + " </where> order by t.id desc</script>")
    List<Map<String, Object>> teachers(@Param("keyword") String keyword);

    @Select("<script>select a.id, a.username, a.name, a.avatar, a.phone, a.email from admin a"
            + " <where>"
            + " <if test='keyword != null'> and (a.name like concat('%', #{keyword}, '%') or a.username like concat('%', #{keyword}, '%'))</if>"
            + " </where> order by a.id desc</script>")
    List<Map<String, Object>> admins(@Param("keyword") String keyword);

    /** 删除教师前的检查：开过的课（含已结课）、带的班 */
    @Select("select count(*) from course where teacher_id = #{teacherId}")
    int countCoursesOfTeacher(Integer teacherId);

    @Select("select count(*) from classes where teacher_id = #{teacherId}")
    int countClassesOfTeacher(Integer teacherId);

    /** 删除学生前的检查：选课记录 */
    @Select("select count(*) from choice where student_id = #{studentId}")
    int countChoicesOfStudent(Integer studentId);
}
