package com.example.mapper;

import com.example.entity.Attendance;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 操作 attendance 相关数据接口（通用增删改查见 {@link CrudMapper}）
 */
public interface AttendanceMapper extends CrudMapper<Attendance> {

    @Select("select * from attendance where student_id = #{studentId} and course_id = #{courseId} and time = #{time}")
    Attendance selectByStudentIdAndCourseIdAndTime(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId, @Param("time") String time);

    /**
     * 考勤状态占比统计：返回 name（考勤状态）与 value（次数）。
     * 分组交给数据库做，并排除 status 为 NULL 的行（内存里 groupingBy 遇到 null key 会抛 NPE）。
     * 过滤条件由实体承载（教师/学生的数据隔离范围）。
     */
    List<Map<String, Object>> selectStatusDistribution(Attendance attendance);
}
