package com.example.mapper;

import com.example.entity.Student;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 学分原子增减：delta 可为负数。
     * 必须走 SQL 自增而不是「查出来 +N 再整行写回」，否则两位教师并发录入同一学生的成绩会互相覆盖（丢更新）。
     */
    @Update("update student set score = ifnull(score, 0) + #{delta} where id = #{id}")
    int addScore(@Param("id") Integer id, @Param("delta") int delta);
}
