package com.example.mapper;

import com.example.entity.OperLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作日志 Mapper（全部使用 #{} 预编译，与项目约定一致）
 */
public interface OperLogMapper {

    @Insert("insert into sys_oper_log (username, module, type, url, params, ip, code, msg, duration, create_time) " +
            "values (#{username}, #{module}, #{type}, #{url}, #{params}, #{ip}, #{code}, #{msg}, #{duration}, now())")
    int insert(OperLog log);

    @Select("<script>" +
            "select * from sys_oper_log" +
            "<where>" +
            "<if test='username != null and username != \"\"'> and username like concat('%', #{username}, '%')</if>" +
            "<if test='module != null and module != \"\"'> and module like concat('%', #{module}, '%')</if>" +
            "</where>" +
            " order by id desc" +
            "</script>")
    List<OperLog> selectAll(OperLog query);

    @Delete("delete from sys_oper_log where id = #{id}")
    int deleteById(Integer id);

    @Delete("<script>" +
            "delete from sys_oper_log where id in" +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int deleteBatch(@Param("ids") List<Integer> ids);

    @Delete("delete from sys_oper_log where create_time < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int deleteOlderThanDays(int days);
}
