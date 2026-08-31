package com.example.controller;

import com.example.entity.Speciality;
import com.example.service.CrudService;
import com.example.service.SpecialityService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 专业信息表前端操作接口（通用增删改查见 {@link CrudController}）
 **/
@RestController
@RequestMapping("/speciality")
public class SpecialityController extends CrudController<Speciality> {

    @Resource
    private SpecialityService specialityService;

    @Override
    protected CrudService<Speciality> getService() {
        return specialityService;
    }
}
