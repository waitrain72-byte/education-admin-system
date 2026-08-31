package com.example.mapper;

import com.example.entity.Score;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 操作 score 相关数据接口（通用增删改查见 {@link CrudMapper}）
 */
public interface ScoreMapper extends CrudMapper<Score> {

    @Select("select * from score where course_id = #{courseId} and student_id = #{studentId}")
    Score selectByCourceIdAndStudentId(@Param("courseId") Integer courseId, @Param("studentId") Integer studentId);
}
