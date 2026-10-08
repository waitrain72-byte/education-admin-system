package com.example.mapper;

import com.example.entity.CourseEval;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 课程评价
 */
public interface CourseEvalMapper {

    @Insert("insert into course_eval (course_id, teacher_id, student_id, attitude, content_score, method, effect, support, comment, create_time)"
            + " values (#{courseId}, #{teacherId}, #{studentId}, #{attitude}, #{contentScore}, #{method}, #{effect}, #{support}, #{comment}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseEval eval);

    @Select("select * from course_eval where course_id = #{courseId} and student_id = #{studentId} limit 1")
    CourseEval selectByCourseAndStudent(@Param("courseId") Integer courseId, @Param("studentId") Integer studentId);

    /** 一门课的全部评价，最新的在前 */
    @Select("select * from course_eval where course_id = #{courseId} order by id desc")
    List<CourseEval> selectByCourse(Integer courseId);
}
