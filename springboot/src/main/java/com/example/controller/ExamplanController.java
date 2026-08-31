package com.example.controller;

import com.example.entity.Examplan;
import com.example.service.CrudService;
import com.example.service.ExamplanService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 考试信息表前端操作接口（通用增删改查见 {@link CrudController}）
 **/
@RestController
@RequestMapping("/examplan")
public class ExamplanController extends CrudController<Examplan> {

    @Resource
    private ExamplanService examplanService;

    @Override
    protected CrudService<Examplan> getService() {
        return examplanService;
    }
}
