package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.RequirePermission;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.service.CourseService;
import com.example.service.CrudService;
import com.example.service.RecommendService;
import com.example.utils.TokenUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 课程信息表前端操作接口（通用增删改查见 {@link CrudController}）
 **/
@RestController
@RequestMapping("/course")
@RequirePermission(module = "course")
public class CourseController extends CrudController<Course> {

    @Resource
    private CourseService courseService;

    @Resource
    private RecommendService recommendService;

    @Override
    protected CrudService<Course> getService() {
        return courseService;
    }

    /**
     * 课程推荐（基于物品的协同过滤）：为当前登录用户生成个性化推荐，
     * 学生按选课相似度推荐，无选课记录（冷启动）降级为按选课人数的热门推荐
     */
    @GetMapping("/recommend")
    public Result recommend(@RequestParam(defaultValue = "5") int limit) {
        Account current = TokenUtils.getCurrentUser();
        int safeLimit = Math.max(1, Math.min(limit, 10));
        return Result.success(recommendService.recommendForStudent(current.getId(), safeLimit));
    }
}
