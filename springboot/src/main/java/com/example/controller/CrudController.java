package com.example.controller;

import com.example.common.Result;
import com.example.service.CrudService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通用实体 REST 基类：统一提供新增/删除/批量删除/修改/按ID查询/查询所有/分页查询 共 7 个标准接口。
 * 子类只需：声明 {@code @RestController @RequestMapping(...)}，并实现 {@link #getService()} 返回对应 Service。
 * 行为差异（如新增/更新后 WebSocket 推送、{@code @NoRepeatSubmit} 防重复提交、额外的导出/统计接口）
 * 在子类中覆盖对应端点或直接新增端点即可。
 */
public abstract class CrudController<T> {

    protected abstract CrudService<T> getService();

    @PostMapping("/add")
    public Result add(@RequestBody T entity) {
        getService().add(entity);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        getService().deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        getService().deleteBatch(ids);
        return Result.success();
    }

    @PutMapping("/update")
    public Result updateById(@RequestBody T entity) {
        getService().updateById(entity);
        return Result.success();
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        return Result.success(getService().selectById(id));
    }

    @GetMapping("/selectAll")
    public Result selectAll(T entity) {
        return Result.success(getService().selectAll(entity));
    }

    @GetMapping("/selectPage")
    public Result selectPage(T entity,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(getService().selectPage(entity, pageNum, pageSize));
    }
}
