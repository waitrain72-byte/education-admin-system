package com.example.mapper;

import com.example.entity.Comment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 操作 comment 相关数据接口（通用增删改查见 {@link CrudMapper}）
 */
public interface CommentMapper extends CrudMapper<Comment> {

    @Select("select * from comment where teacher = #{teacher} and name = #{name} and student = #{student}")
    Comment selectByTeacherAndCourseAndStudent(@Param("teacher") String teacher, @Param("name") String name, @Param("student") String student);
}
