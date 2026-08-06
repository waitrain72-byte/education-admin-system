package com.example.mapper;

import com.example.entity.Admin;
import org.apache.ibatis.annotations.Select;

/**
 * 操作 admin 相关数据接口
 */
public interface AdminMapper extends BaseMapper<Admin> {

    @Override
    @Select("select * from admin where username = #{username}")
    Admin selectByUsername(String username);

    @Override
    @Select("select count(*) from admin where avatar = #{avatar}")
    int countByAvatar(String avatar);
}
