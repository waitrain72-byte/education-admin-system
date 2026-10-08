package com.example.mapper;

import com.example.entity.Choice;
import com.example.entity.Course;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 操作 choice 相关数据接口（通用增删改查见 {@link CrudMapper}）
 */
public interface ChoiceMapper extends CrudMapper<Choice> {

    /** 该门课已选人数：只要计数就别把整张选课记录捞回来再 size() */
    @Select("select count(*) from choice where course_id = #{courseId}")
    int countByCourseId(Integer courseId);

    /** 某学生是否选了某门课（课程空间按它判断学生能不能进） */
    @Select("select count(*) from choice where student_id = #{studentId} and course_id = #{courseId}")
    int countByStudentAndCourse(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    /**
     * 该学生已选且未结课课程的上课时段（一次 join 取回，替代「逐条 selectById」的 N+1）。
     * 返回的 Course 只保证 name/week/segment 可用，供选课时间冲突判断使用。
     */
    @Select("select c.id, c.name, c.week, c.segment from choice ch "
            + "join course c on ch.course_id = c.id "
            + "where ch.student_id = #{studentId} and ifnull(c.status, '') <> '已结课'")
    List<Course> selectActiveSlotsByStudentId(Integer studentId);

    /** 该学生选了的全部课程 ID（课程广场标「已选」用） */
    @Select("select course_id from choice where student_id = #{studentId}")
    List<Integer> selectCourseIdsByStudentId(Integer studentId);

    /** 课程换了任课教师：选课记录上冗余的教师一起改（旧版按 choice.teacher_id 筛「我的学生」） */
    @Update("update choice set teacher_id = #{teacherId} where course_id = #{courseId}")
    int updateTeacherOfCourse(@Param("courseId") Integer courseId, @Param("teacherId") Integer teacherId);

    /** 退选：同一学生同一门课的选课记录（历史数据里可能重复）一并删除 */
    @Delete("delete from choice where student_id = #{studentId} and course_id = #{courseId}")
    int deleteByStudentAndCourse(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);
}
