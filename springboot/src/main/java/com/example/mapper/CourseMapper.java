package com.example.mapper;

import com.example.entity.Course;
import org.apache.ibatis.annotations.Select;

/**
 * 操作 course 相关数据接口（通用增删改查见 {@link CrudMapper}）
 */
public interface CourseMapper extends CrudMapper<Course> {

    /**
     * 带行锁读取课程（须在事务内调用）。
     * 选课的「查容量 → 判冲突 → insert」若不锁住课程行，并发请求会各自读到未满的人数而同时插入，造成超额选课。
     */
    @Select("select * from course where id = #{id} for update")
    Course selectByIdForUpdate(Integer id);

    /**
     * 教室占用查询：返回同一「教室 + 星期 + 大节」已存在的课程（无则返回 null）。
     * 查询条件由实体承载：room/week/segment 为匹配字段，id 为排除自身的可变字段（更新时传自身 id）。
     * 供保存前校验（CourseService）与表单实时提示（/course/roomOccupied）使用。
     */
    Course selectRoomOccupied(Course course);
}
