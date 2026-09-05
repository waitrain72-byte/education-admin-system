package com.example.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通用实体 Mapper：为所有非账户业务实体提供统一的增删改查方法签名。
 * 具体 SQL 仍由各表对应的 XML 提供（MyBatis 按方法名绑定到同名 statement）。
 * 子接口无需重复声明通用 CRUD，仅需补充业务特有的查询方法（如按课程/学生查询）。
 * 注：既有 BaseMapper 专用于管理员/教师/学生三类账号（含 username/avatar 维度），本接口面向其他业务实体。
 */
public interface CrudMapper<T> {

    int insert(T entity);

    int deleteById(Integer id);

    /**
     * 批量删除：单条 IN 语句，替代循环逐条删除（空集合由 Service 层拦截，不会生成非法 SQL）
     */
    int deleteBatchIds(@Param("ids") List<Integer> ids);

    int updateById(T entity);

    T selectById(Integer id);

    List<T> selectAll(T entity);
}
