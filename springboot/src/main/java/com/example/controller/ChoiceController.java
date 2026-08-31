package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.NoRepeatSubmit;
import com.example.entity.Choice;
import com.example.entity.Curriculum;
import com.example.service.ChoiceService;
import com.example.service.CrudService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 选课信息表前端操作接口（通用增删改查见 {@link CrudController}）
 **/
@RestController
@RequestMapping("/choice")
public class ChoiceController extends CrudController<Choice> {

    @Resource
    private ChoiceService choiceService;

    @Override
    protected CrudService<Choice> getService() {
        return choiceService;
    }

    /**
     * 新增（选课，防重复提交）
     */
    @Override
    @NoRepeatSubmit
    @PostMapping("/add")
    public Result add(@RequestBody Choice choice) {
        return super.add(choice);
    }

    /**
     * 生成对应学生的选课课表
     */
    @GetMapping("/getCurriculum")
    public Result getCurriculum() {
        List<Curriculum> list = choiceService.getCurriculum();
        return Result.success(list);
    }
}
