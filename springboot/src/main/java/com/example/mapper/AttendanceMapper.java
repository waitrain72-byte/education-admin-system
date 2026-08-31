package com.example.mapper;

import com.example.entity.Attendance;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 操作 attendance 相关数据接口（通用增删改查见 {@link CrudMapper}）
 */
public interface AttendanceMapper extends CrudMapper<Attendance> {

    @Select("select * from attendance where student_id = #{studentId} and course_id = #{courseId} and time = #{time}")
    Attendance selectByStudentIdAndCourseIdAndTime(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId, @Param("time") String time);
}
