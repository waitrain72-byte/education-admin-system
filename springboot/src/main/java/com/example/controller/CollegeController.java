package com.example.controller;

import com.example.common.annotation.RequirePermission;
import com.example.entity.College;
import com.example.service.CollegeService;
import com.example.service.CrudService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 学院信息表前端操作接口（通用增删改查见 {@link CrudController}）
 **/
@RestController
@RequestMapping("/college")
@RequirePermission(module = "college")
public class CollegeController extends CrudController<College> {

    @Resource
    private CollegeService collegeService;

    @Override
    protected CrudService<College> getService() {
        return collegeService;
    }
}
