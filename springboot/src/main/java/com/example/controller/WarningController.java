package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.RequirePermission;
import com.example.service.WarningService;
import com.example.websocket.NoticeWebSocketServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 学业预警接口：基于成绩与考勤的多指标加权评分模型。
 * 类级权限 module = "score"：GET 派生 score:view（查看预警），
 * POST 派生 score:manage（推送提醒，仅教师/管理员具备）。
 */
@RestController
@RequestMapping("/warning")
@RequirePermission(module = "score")
public class WarningController {

    @Resource
    private WarningService warningService;

    /**
     * 学业预警列表：管理员看全部、教师看本人任课学生、学生仅看本人
     */
    @GetMapping("/list")
    public Result list() {
        return Result.success(warningService.listWarnings());
    }

    /**
     * 向学生实时推送学业预警提醒（WebSocket，App 端 toast + 角标，Web 端弹窗）
     */
    @PostMapping("/notify/{studentId}")
    public Result notify(@PathVariable Integer studentId) {
        Map<String, Object> target = null;
        for (Map<String, Object> row : warningService.listWarnings()) {
            if (studentId.equals(row.get("studentId"))) {
                target = row;
                break;
            }
        }
        if (target == null) {
            return Result.error("400", "该学生当前无预警数据");
        }
        NoticeWebSocketServer.sendToUser(studentId, "STUDENT", "学业预警提醒",
                "你的学业风险指数 " + target.get("riskIndex") + "（" + target.get("level") + "）：" + target.get("suggestion"));
        return Result.success();
    }
}
