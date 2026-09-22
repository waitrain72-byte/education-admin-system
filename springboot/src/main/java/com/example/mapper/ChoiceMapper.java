package com.example.mapper;

import com.example.entity.Choice;
import com.example.entity.Course;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作 choice 相关数据接口（通用增删改查见 {@link CrudMapper}）
 */
public interface ChoiceMapper extends CrudMapper<Choice> {

    /** 该门课已选人数：只要计数就别把整张选课记录捞回来再 size() */
    @Select("select count(*) from choice where course_id = #{courseId}")
    int countByCourseId(Integer courseId);

    /**
     * 该学生已选且未结课课程的上课时段（一次 join 取回，替代「逐条 selectById」的 N+1）。
     * 返回的 Course 只保证 name/week/segment 可用，供选课时间冲突判断使用。
     */
    @Select("select c.name, c.week, c.segment from choice ch "
            + "join course c on ch.course_id = c.id "
            + "where ch.student_id = #{studentId} and ifnull(c.status, '') <> '已结课'")
    List<Course> selectActiveSlotsByStudentId(Integer studentId);
}
