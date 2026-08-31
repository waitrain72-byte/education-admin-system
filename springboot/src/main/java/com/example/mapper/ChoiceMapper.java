package com.example.mapper;

import com.example.entity.Choice;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作 choice 相关数据接口（通用增删改查见 {@link CrudMapper}）
 */
public interface ChoiceMapper extends CrudMapper<Choice> {

    @Select("select * from choice where course_id = #{courseId}")
    List<Choice> selectByCourseId(Integer courseId);

    @Select("select * from choice where student_id = #{studentId}")
    List<Choice> selectByStudentId(Integer studentId);
}
