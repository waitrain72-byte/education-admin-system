package com.example.mapper;

import com.example.entity.LoginLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 登录日志 Mapper
 */
public interface LoginLogMapper {

    @Insert("insert into sys_login_log (username, ip, status, msg, create_time) " +
            "values (#{username}, #{ip}, #{status}, #{msg}, now())")
    int insert(LoginLog log);

    @Select("<script>" +
            "select * from sys_login_log" +
            "<where>" +
            "<if test='username != null and username != \"\"'> and username like concat('%', #{username}, '%')</if>" +
            "<if test='status != null and status != \"\"'> and status = #{status}</if>" +
            "</where>" +
            " order by id desc" +
            "</script>")
    List<LoginLog> selectAll(LoginLog query);

    @Delete("delete from sys_login_log where id = #{id}")
    int deleteById(Integer id);

    @Delete("<script>" +
            "delete from sys_login_log where id in" +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int deleteBatch(@Param("ids") List<Integer> ids);

    @Delete("delete from sys_login_log where create_time < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int deleteOlderThanDays(int days);
}
