package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.RequirePermission;
import com.example.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 数据大屏聚合统计接口（一次性返回大屏所需的全部指标）。
 * 统计与缓存逻辑见 {@link DashboardService}。
 */
@RestController
@RequestMapping("/dashboard")
@RequirePermission(module = "dashboard")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public Result stats() {
        return Result.success(dashboardService.stats());
    }
}
