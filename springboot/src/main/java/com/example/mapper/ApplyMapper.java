package com.example.mapper;

import com.example.entity.Apply;
import org.apache.ibatis.annotations.Select;

/**
 * 操作 apply 相关数据接口（通用增删改查见 {@link CrudMapper}）
 */
public interface ApplyMapper extends CrudMapper<Apply> {

    @Select("select * from apply where username = #{username}")
    Apply selectByUsername(String username);
}
