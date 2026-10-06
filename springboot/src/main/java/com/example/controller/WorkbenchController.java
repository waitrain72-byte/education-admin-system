package com.example.controller;

import com.example.common.Result;
import com.example.service.SearchService;
import com.example.service.WorkbenchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 首页与全局搜索：内容按当前登录角色裁剪（数据范围在 Service 里按角色收窄），登录即可调用
 */
@RestController
public class WorkbenchController {

    @Resource
    private WorkbenchService workbenchService;
    @Resource
    private SearchService searchService;

    /** 首页聚合数据（学生 / 教师 / 管理员各不相同） */
    @GetMapping("/workbench/summary")
    public Result summary() {
        return Result.success(workbenchService.summary());
    }

    /** 全局搜索（顶栏 Ctrl K）：课程、通知；管理员额外可搜学生与教师 */
    @GetMapping("/search")
    public Result search(@RequestParam(defaultValue = "") String keyword) {
        return Result.success(searchService.search(keyword));
    }
}
