package com.example.mapper;

import com.example.entity.Assignment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 作业任务
 */
public interface AssignmentMapper {

    @Insert("insert into assignment (course_id, teacher_id, title, content, attachment, attachment_name, deadline, full_score, create_time)"
            + " values (#{courseId}, #{teacherId}, #{title}, #{content}, #{attachment}, #{attachmentName}, #{deadline}, #{fullScore}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Assignment assignment);

    /** 课程、布置教师、布置时间不随修改变化 */
    @Update("update assignment set title = #{title}, content = #{content}, attachment = #{attachment},"
            + " attachment_name = #{attachmentName}, deadline = #{deadline}, full_score = #{fullScore} where id = #{id}")
    int update(Assignment assignment);

    @Delete("delete from assignment where id = #{id}")
    int deleteById(Integer id);

    @Select("select * from assignment where id = #{id}")
    Assignment selectById(Integer id);

    /** 一门课的全部作业，最近布置的在前 */
    @Select("select * from assignment where course_id = #{courseId} order by id desc")
    List<Assignment> selectByCourse(Integer courseId);
}
