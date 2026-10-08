package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.NoRepeatSubmit;
import com.example.common.annotation.RequirePermission;
import com.example.entity.CourseEval;
import com.example.service.CourseEvalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 课程空间 · 课程评价。权限沿用评教模块：查看 comment:view，提交 comment:manage（学生）。
 */
@RestController
@RequestMapping("/course/{courseId}/evaluations")
@RequirePermission(module = "comment")
public class CourseEvalController {

    @Resource
    private CourseEvalService courseEvalService;

    @GetMapping
    public Result view(@PathVariable Integer courseId) {
        return Result.success(courseEvalService.view(courseId));
    }

    @NoRepeatSubmit
    @PostMapping
    public Result submit(@PathVariable Integer courseId, @RequestBody CourseEval form) {
        return Result.success(courseEvalService.submit(courseId, form));
    }
}
