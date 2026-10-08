package com.example.controller;

import cn.hutool.core.util.StrUtil;
import com.example.common.Result;
import com.example.common.annotation.NoRepeatSubmit;
import com.example.common.annotation.RequirePermission;
import com.example.entity.Apply;
import com.example.service.ApplyService;
import com.example.service.CrudService;
import com.example.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @Resource
    private MessageService messageService;

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
        boolean reviewed = applyService.update(apply);
        if (reviewed) {
            // 学生、日期以库里为准（审核请求可能只带了 id、状态和意见）
            Apply saved = applyService.selectById(apply.getId());
            String month = saved.getTime() != null && saved.getTime().length() >= 7 ? saved.getTime().substring(0, 7) : "";
            messageService.push(saved.getStudentId(), "STUDENT", "apply",
                    "请假审核结果", "你的请假申请" + saved.getStatus()
                            + (StrUtil.isBlank(saved.getDescr()) ? "" : "：" + saved.getDescr()),
                    "/schedule?view=month" + (month.isEmpty() ? "" : "&month=" + month));
        }
        return Result.success();
    }

    /** 请假预览：从 from 起连续 days 天里，当前学生要上的课 */
    @GetMapping("/preview")
    public Result preview(@RequestParam String from, @RequestParam(required = false) Integer days) {
        return Result.success(applyService.preview(from, days));
    }
}
