package com.example.mapper;

import com.example.entity.CourseResource;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 课程资料
 */
public interface CourseResourceMapper {

    @Insert("insert into course_resource (course_id, name, file, size, uploader_id, uploader_role, create_time)"
            + " values (#{courseId}, #{name}, #{file}, #{size}, #{uploaderId}, #{uploaderRole}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseResource resource);

    @Select("select * from course_resource where id = #{id}")
    CourseResource selectById(Integer id);

    /** 一门课的资料，带上传人姓名（老师或管理员），最新上传的在前 */
    @Select("select r.*, coalesce(t.name, a.name) as uploaderName from course_resource r"
            + " left join teacher t on r.uploader_role = 'TEACHER' and r.uploader_id = t.id"
            + " left join admin a on r.uploader_role = 'ADMIN' and r.uploader_id = a.id"
            + " where r.course_id = #{courseId} order by r.id desc")
    List<CourseResource> selectByCourse(Integer courseId);

    @Delete("delete from course_resource where id = #{id}")
    int deleteById(Integer id);
}
