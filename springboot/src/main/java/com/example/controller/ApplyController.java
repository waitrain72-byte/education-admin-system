package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.NoRepeatSubmit;
import com.example.common.annotation.RequirePermission;
import com.example.entity.Apply;
import com.example.service.ApplyService;
import com.example.service.CrudService;
import com.example.websocket.NoticeWebSocketServer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 请假信息表前端操作接口（通用增删改查见 {@link CrudController}）
 **/
@RestController
@RequestMapping("/apply")
@RequirePermission(module = "apply")
public class ApplyController extends CrudController<Apply> {

    @Resource
    private ApplyService applyService;

    @Override
    protected CrudService<Apply> getService() {
        return applyService;
    }

    /**
     * 新增（防重复提交）
     */
    @Override
    @NoRepeatSubmit
    @PostMapping("/add")
    public Result add(@RequestBody Apply apply) {
        return super.add(apply);
    }

    /**
     * 修改（学生撤销/重新提交，管理员审核；审核结果实时推送学生）
     */
    @Override
    @NoRepeatSubmit
    @PutMapping("/update")
    public Result updateById(@RequestBody Apply apply) {
        applyService.updateById(apply);
        if ("审核通过".equals(apply.getStatus()) || "审核不通过".equals(apply.getStatus())) {
            NoticeWebSocketServer.sendToUser(apply.getStudentId(), "STUDENT",
                    "请假审核结果", "你的请假申请" + apply.getStatus()
                            + (apply.getDescr() == null ? "" : "：" + apply.getDescr()));
        }
        return Result.success();
    }
}
