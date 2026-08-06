package com.example.mapper;

import com.example.entity.Teacher;
import org.apache.ibatis.annotations.Select;

/**
 * 操作 teacher 相关数据接口
 */
public interface TeacherMapper extends BaseMapper<Teacher> {

    @Override
    @Select("select * from teacher where username = #{username}")
    Teacher selectByUsername(String username);

    @Override
    @Select("select count(*) from teacher where avatar = #{avatar}")
    int countByAvatar(String avatar);
}
