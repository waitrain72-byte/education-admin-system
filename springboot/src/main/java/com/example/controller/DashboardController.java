package com.example.controller;

import com.example.common.Result;
import com.example.mapper.DashboardMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据大屏聚合统计接口（一次性返回大屏所需的全部指标）
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

    @Resource
    private DashboardMapper dashboardMapper;

    @GetMapping("/stats")
    public Result stats() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 指标卡
        data.put("studentCount", dashboardMapper.countStudent());
        data.put("teacherCount", dashboardMapper.countTeacher());
        data.put("courseCount", dashboardMapper.countCourse());
        data.put("choiceCount", dashboardMapper.countChoice());
        data.put("loginToday", dashboardMapper.countLoginToday());
        data.put("loginWeek", dashboardMapper.countLoginWeek());
        data.put("pendingApply", dashboardMapper.countPendingApply());
        data.put("ungradedHomework", dashboardMapper.countUngradedHomework());

        // 分布图
        data.put("collegeDist", dashboardMapper.collegeDist());
        data.put("courseTop", dashboardMapper.courseTop());
        data.put("titleDist", dashboardMapper.titleDist());

        // 近 7 天登录趋势（后端补零，保证连续 7 天）
        Map<String, Long> trendMap = new LinkedHashMap<>();
        for (Map<String, Object> row : dashboardMapper.loginTrend()) {
            Object date = row.get("date");
            trendMap.put(date == null ? "" : String.valueOf(date), ((Number) row.get("value")).longValue());
        }
        LocalDate today = LocalDate.now();
        java.util.List<Map<String, Object>> trend = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String day = today.minusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", LocalDate.parse(day).format(DAY_FORMAT));
            point.put("value", trendMap.getOrDefault(day, 0L));
            trend.add(point);
        }
        data.put("loginTrend", trend);

        return Result.success(data);
    }
}
