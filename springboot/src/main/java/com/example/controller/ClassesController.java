package com.example.controller;

import com.example.common.annotation.RequirePermission;
import com.example.entity.Classes;
import com.example.service.ClassesService;
import com.example.service.CrudService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 班级信息表前端操作接口（通用增删改查见 {@link CrudController}）
 **/
@RestController
@RequestMapping("/classes")
@RequirePermission(module = "classes")
public class ClassesController extends CrudController<Classes> {

    @Resource
    private ClassesService classesService;

    @Override
    protected CrudService<Classes> getService() {
        return classesService;
    }
}
