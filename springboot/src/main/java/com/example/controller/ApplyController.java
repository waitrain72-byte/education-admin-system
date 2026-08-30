package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.NoRepeatSubmit;
import com.example.entity.Apply;
import com.example.service.ApplyService;
import com.example.websocket.NoticeWebSocketServer;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 请假信息表前端操作接口
 **/
@RestController
@RequestMapping("/apply")
public class ApplyController {

    @Resource
    private ApplyService applyService;

    /**
     * 新增
     */
    @NoRepeatSubmit
    @PostMapping("/add")
    public Result add(@RequestBody Apply apply) {
        applyService.add(apply);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        applyService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        applyService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改（学生撤销/重新提交，管理员审核；审核结果实时推送学生）
     */
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

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Apply apply = applyService.selectById(id);
        return Result.success(apply);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Apply apply ) {
        List<Apply> list = applyService.selectAll(apply);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Apply apply,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Apply> page = applyService.selectPage(apply, pageNum, pageSize);
        return Result.success(page);
    }

}