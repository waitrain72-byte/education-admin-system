package com.example.mapper;

import com.example.entity.Account;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通用账号 Mapper：管理员/教师/学生三类账号拥有相同的数据操作方法。
 * 具体语句由各表对应的 XML 提供，selectByUsername / countByAvatar 由子接口以注解实现。
 */
public interface BaseMapper<T extends Account> {

    int insert(T entity);

    int deleteById(Integer id);

    /**
     * 批量删除：单条 IN 语句，替代循环逐条删除（空集合由 Service 层拦截，不会生成非法 SQL）
     */
    int deleteBatchIds(@Param("ids") List<Integer> ids);

    int updateById(T entity);

    T selectById(Integer id);

    List<T> selectAll(T entity);

    T selectByUsername(String username);

    int countByAvatar(String avatar);
}
