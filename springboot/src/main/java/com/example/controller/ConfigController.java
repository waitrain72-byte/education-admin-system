package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.RequirePermission;
import com.example.entity.SemesterConfig;
import com.example.service.ConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 系统参数接口：学期设置
 */
@RestController
@RequestMapping("/config")
public class ConfigController {

    @Resource
    private ConfigService configService;

    /** 当前学期与教学周（登录即可读，首页与日程都要用） */
    @GetMapping("/semester")
    public Result semester() {
        return Result.success(configService.semester());
    }

    /** 修改学期设置（仅管理员） */
    @RequirePermission("config:manage")
    @PutMapping("/semester")
    public Result updateSemester(@RequestBody SemesterConfig config) {
        configService.updateSemester(config);
        return Result.success(configService.semester());
    }
}
