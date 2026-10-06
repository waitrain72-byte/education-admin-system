package com.example.mapper;

import com.example.entity.Student;
import com.example.entity.Teacher;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 全局搜索的人员查询（姓名或账号模糊匹配，最多 6 条，只取展示需要的列，不含密码）
 */
public interface SearchMapper {

    @Select("select s.id, s.username, s.name, s.avatar, cl.name as className from student s"
            + " left join classes cl on s.class_id = cl.id"
            + " where s.name like concat('%', #{keyword}, '%') or s.username like concat('%', #{keyword}, '%')"
            + " order by s.id desc limit 6")
    List<Student> searchStudents(String keyword);

    @Select("select id, username, name, avatar, title from teacher"
            + " where name like concat('%', #{keyword}, '%') or username like concat('%', #{keyword}, '%')"
            + " order by id desc limit 6")
    List<Teacher> searchTeachers(String keyword);
}
