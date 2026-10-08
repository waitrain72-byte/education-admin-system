package com.example.mapper;

import com.example.entity.AttendanceSession;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 课堂签到场次
 */
public interface AttendanceSessionMapper {

    @Insert("insert into attendance_session (course_id, teacher_id, code, date, start_time, expire_time, status)"
            + " values (#{courseId}, #{teacherId}, #{code}, #{date}, #{startTime}, #{expireTime}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AttendanceSession session);

    @Select("select * from attendance_session where id = #{id}")
    AttendanceSession selectById(Integer id);

    /** 一门课当前「进行中」的场次（是否已过截止时间由 Service 判断） */
    @Select("select * from attendance_session where course_id = #{courseId} and status = '进行中' order by id desc limit 1")
    AttendanceSession selectActiveByCourse(Integer courseId);

    /** 已过截止时间却还标着进行中的场次（定时任务收尾用） */
    @Select("select * from attendance_session where status = '进行中' and expire_time < #{now}")
    List<AttendanceSession> selectExpired(String now);

    /** 结束场次：只改还在进行中的，返回值为 0 说明已被别的请求结束过（并发收尾不会重复处理） */
    @Update("update attendance_session set status = '已结束', expire_time = #{expireTime} where id = #{id} and status = '进行中'")
    int finish(@Param("id") Integer id, @Param("expireTime") String expireTime);

    /** 给定课程里正在签到（未过截止时间）的课程 ID，首页「去签到」提示用 */
    @Select("<script>select distinct course_id from attendance_session where status = '进行中' and expire_time &gt;= #{now}"
            + " and course_id in <foreach collection='courseIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Integer> selectSigningCourseIds(@Param("courseIds") List<Integer> courseIds, @Param("now") String now);
}
