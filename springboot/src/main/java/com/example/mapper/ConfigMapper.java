package com.example.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统参数（sys_config 键值表）
 */
public interface ConfigMapper {

    @Select("select config_value from sys_config where config_key = #{key}")
    String selectValue(String key);

    /** 不存在则插入、存在则覆盖值（说明保持不变） */
    @Insert("insert into sys_config (config_key, config_value) values (#{key}, #{value})"
            + " on duplicate key update config_value = values(config_value)")
    int upsert(@Param("key") String key, @Param("value") String value);
}
