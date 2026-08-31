package com.example.controller;

import com.example.common.annotation.RequirePermission;
import com.example.entity.Course;
import com.example.service.CourseService;
import com.example.service.CrudService;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @Override
    protected CrudService<Course> getService() {
        return courseService;
    }
}
