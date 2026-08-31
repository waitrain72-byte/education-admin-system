package com.example.controller;

import com.example.common.Result;
import com.example.entity.Attendance;
import com.example.service.AttendanceService;
import com.example.service.CrudService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 考勤信息表前端操作接口（通用增删改查见 {@link CrudController}）
 **/
@RestController
@RequestMapping("/attendance")
public class AttendanceController extends CrudController<Attendance> {

    @Resource
    private AttendanceService attendanceService;

    @Override
    protected CrudService<Attendance> getService() {
        return attendanceService;
    }

    /**
     * 首页 Echarts：考勤状态统计（饼图）
     */
    @GetMapping("/getPie")
    public Result getPie() {
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();
        List<Attendance> all = attendanceService.selectAll(new Attendance());
        Map<String, List<Attendance>> collect = all.stream().collect(Collectors.groupingBy(Attendance::getStatus));
        for (String key : collect.keySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", key);
            map.put("value", collect.get(key).size());
            list.add(map);
        }
        resultMap.put("text", "考勤状态统计图（饼图）");
        resultMap.put("subtext", "统计维度：考勤状态");
        resultMap.put("name", "考勤状态");
        resultMap.put("data", list);
        return Result.success(resultMap);
    }
}
