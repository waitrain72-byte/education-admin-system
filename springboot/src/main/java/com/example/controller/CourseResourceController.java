package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.RequirePermission;
import com.example.entity.CourseResource;
import com.example.service.CourseResourceService;
import com.example.service.CourseSpaceService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 课程空间 · 课程资料。权限：查看 resource:view，上传登记 / 删除 resource:manage。
 * 文件先走 /files/upload 上传，再把返回的地址和文件名、大小登记到这里。
 */
@RestController
@RequestMapping("/course/{courseId}/resources")
@RequirePermission(module = "resource")
public class CourseResourceController {

    @Resource
    private CourseResourceService courseResourceService;
    @Resource
    private CourseSpaceService courseSpaceService;

    @GetMapping
    public Result list(@PathVariable Integer courseId) {
        return Result.success(courseResourceService.list(courseId));
    }

    @PostMapping
    public Result add(@PathVariable Integer courseId, @RequestBody CourseResource form) {
        CourseResource saved = courseResourceService.add(courseId, form);
        courseSpaceService.emit(courseId, "resources");
        return Result.success(saved);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer courseId, @PathVariable Integer id) {
        courseResourceService.delete(courseId, id);
        courseSpaceService.emit(courseId, "resources");
        return Result.success();
    }
}
