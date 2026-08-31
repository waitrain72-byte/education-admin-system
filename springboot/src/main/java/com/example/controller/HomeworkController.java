package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.NoRepeatSubmit;
import com.example.common.annotation.RequirePermission;
import com.example.entity.Homework;
import com.example.service.CrudService;
import com.example.service.HomeworkService;
import com.example.websocket.NoticeWebSocketServer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 作业前端操作接口（通用增删改查见 {@link CrudController}）
 **/
@RestController
@RequestMapping("/homework")
@RequirePermission(module = "homework")
public class HomeworkController extends CrudController<Homework> {

    @Resource
    private HomeworkService homeworkService;

    @Override
    protected CrudService<Homework> getService() {
        return homeworkService;
    }

    /**
     * 新增（提交作业实时通知教师，防重复提交）
     */
    @Override
    @NoRepeatSubmit
    @PostMapping("/add")
    public Result add(@RequestBody Homework homework) {
        homeworkService.add(homework);
        if (homework.getTeacherId() != null) {
            NoticeWebSocketServer.sendToUser(homework.getTeacherId(), "TEACHER",
                    "作业提交通知", "学生提交了新的作业，请到【作业提交】页面查看");
        }
        return Result.success();
    }

    /**
     * 修改（教师批改实时推送学生，防重复提交）
     */
    @Override
    @NoRepeatSubmit
    @PutMapping("/update")
    public Result updateById(@RequestBody Homework homework) {
        homeworkService.updateById(homework);
        if (homework.getScore() != null && homework.getStudentId() != null) {
            NoticeWebSocketServer.sendToUser(homework.getStudentId(), "STUDENT",
                    "作业批改通知", "你提交的作业已批改，得分：" + homework.getScore());
        }
        return Result.success();
    }
}
