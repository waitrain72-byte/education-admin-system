package com.example.mapper;

import com.example.entity.Student;
import org.apache.ibatis.annotations.Select;

/**
 * 操作 student 相关数据接口
 */
public interface StudentMapper extends BaseMapper<Student> {

    @Override
    @Select("select * from student where username = #{username}")
    Student selectByUsername(String username);

    @Override
    @Select("select count(*) from student where avatar = #{avatar}")
    int countByAvatar(String avatar);
}
