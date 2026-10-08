package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.RequirePermission;
import com.example.service.ScheduleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 日程：周课表、月历（课程、考试、作业截止、请假）。只读，有课程查看权限即可。
 */
@RestController
@RequestMapping("/schedule")
@RequirePermission("course:view")
public class ScheduleController {

    @Resource
    private ScheduleService scheduleService;

    /** 含 date（yyyy-MM-dd，默认今天）的那一周 */
    @GetMapping("/week")
    public Result week(@RequestParam(required = false) String date) {
        return Result.success(scheduleService.week(date));
    }

    /** 某个月（yyyy-MM，默认本月） */
    @GetMapping("/month")
    public Result month(@RequestParam(required = false) String month) {
        return Result.success(scheduleService.month(month));
    }
}
