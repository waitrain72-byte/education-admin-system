package com.example.service;

import com.example.mapper.CrudMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 通用实体业务基类：统一实现增删改查与分页，消除各业务实体 Service 中重复的样板代码。
 * 子类只需通过 {@link #getMapper()} 返回对应的 Mapper；
 * 行为差异（如按角色过滤数据、额外校验、学分计算、WebSocket 推送等）在子类中覆盖对应方法。
 * 注：既有 {@link BaseService} 专用于管理员/教师/学生三类账号（含登录/改密/头像/语言主题/重置密码），本类面向其他业务实体。
 */
public abstract class CrudService<T> {

    protected abstract CrudMapper<T> getMapper();

    public void add(T entity) {
        getMapper().insert(entity);
    }

    public void deleteById(Integer id) {
        getMapper().deleteById(id);
    }

    /**
     * 批量删除：单条 IN 语句。空集合直接返回（否则 foreach 会生成非法的 IN ()）。
     */
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        getMapper().deleteBatchIds(ids);
    }

    public void updateById(T entity) {
        getMapper().updateById(entity);
    }

    public T selectById(Integer id) {
        return getMapper().selectById(id);
    }

    /**
     * 数据行级隔离钩子：查询前按当前登录角色收窄查询范围。
     * 默认不过滤（全量）；需要按角色隔离的子类覆盖本方法，
     * 如成绩/考勤/作业等教师只看本人课程、学生只看本人数据。
     * selectAll 与 selectPage 统一经由本方法，防止绕过分页接口
     * （如 /selectAll、导出、统计图）拿到全量数据。
     */
    protected void applyDataScope(T entity) {
    }

    public List<T> selectAll(T entity) {
        applyDataScope(entity);
        return getMapper().selectAll(entity);
    }

    /**
     * 分页查询：先启动 PageHelper，再查询全量，由 PageHelper 拦截生成 count 与分页 SQL。
     */
    public PageInfo<T> selectPage(T entity, Integer pageNum, Integer pageSize) {
        applyDataScope(entity);
        PageHelper.startPage(pageNum, pageSize);
        List<T> list = getMapper().selectAll(entity);
        return PageInfo.of(list);
    }
}
