package com.example.controller;

import com.example.common.Result;
import com.example.entity.LoginLog;
import com.example.entity.OperLog;
import com.example.service.LogService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 日志查询接口（仅管理员使用，前端页面：操作日志 / 登录日志）
 */
@RestController
public class LogController {

    @Resource
    private LogService logService;

    // ================= 操作日志 =================

    @GetMapping("/operlog/selectPage")
    public Result selectOperPage(OperLog query,
                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<OperLog> page = logService.selectOperPage(query, pageNum, pageSize);
        return Result.success(page);
    }

    @DeleteMapping("/operlog/delete/{id}")
    public Result deleteOperById(@PathVariable Integer id) {
        logService.deleteOperById(id);
        return Result.success();
    }

    @DeleteMapping("/operlog/delete/batch")
    public Result deleteOperBatch(@RequestBody List<Integer> ids) {
        logService.deleteOperBatch(ids);
        return Result.success();
    }

    // ================= 登录日志 =================

    @GetMapping("/loginlog/selectPage")
    public Result selectLoginPage(LoginLog query,
                                  @RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<LoginLog> page = logService.selectLoginPage(query, pageNum, pageSize);
        return Result.success(page);
    }

    @DeleteMapping("/loginlog/delete/{id}")
    public Result deleteLoginById(@PathVariable Integer id) {
        logService.deleteLoginById(id);
        return Result.success();
    }

    @DeleteMapping("/loginlog/delete/batch")
    public Result deleteLoginBatch(@RequestBody List<Integer> ids) {
        logService.deleteLoginBatch(ids);
        return Result.success();
    }
}
