package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.RequirePermission;
import com.example.service.OrgService;
import com.example.service.PeopleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 教务后台：人员（学生 / 教师 / 管理员三类账号一页查）与组织架构树。管理员专用。
 */
@RestController
@RequestMapping("/people")
@RequirePermission("admin:view")
public class PeopleController {

    @Resource
    private PeopleService peopleService;
    @Resource
    private OrgService orgService;

    @GetMapping("/students")
    public Result students(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Integer collegeId,
                           @RequestParam(required = false) Integer specialityId,
                           @RequestParam(required = false) Integer classId,
                           @RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(peopleService.students(keyword, collegeId, specialityId, classId, pageNum, pageSize));
    }

    @GetMapping("/teachers")
    public Result teachers(@RequestParam(required = false) String keyword,
                           @RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(peopleService.teachers(keyword, pageNum, pageSize));
    }

    @GetMapping("/admins")
    public Result admins(@RequestParam(required = false) String keyword,
                         @RequestParam(defaultValue = "1") Integer pageNum,
                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(peopleService.admins(keyword, pageNum, pageSize));
    }

    /** 组织架构树：学院 → 专业 → 班级，带各级学生人数 */
    @GetMapping("/org")
    public Result org() {
        return Result.success(orgService.tree());
    }
}
