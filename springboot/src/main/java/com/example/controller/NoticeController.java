package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.NoRepeatSubmit;
import com.example.common.annotation.RequirePermission;
import com.example.entity.Notice;
import com.example.service.CrudService;
import com.example.service.NoticeService;
import com.example.websocket.NoticeWebSocketServer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 公告信息表前端操作接口（通用增删改查见 {@link CrudController}）
 **/
@RestController
@RequestMapping("/notice")
@RequirePermission(module = "notice")
public class NoticeController extends CrudController<Notice> {

    @Resource
    private NoticeService noticeService;

    @Override
    protected CrudService<Notice> getService() {
        return noticeService;
    }

    /**
     * 新增（全员实时广播，防重复提交）
     */
    @Override
    @NoRepeatSubmit
    @PostMapping("/add")
    public Result add(@RequestBody Notice notice) {
        noticeService.add(notice);
        NoticeWebSocketServer.sendToAll("新教务通知", notice.getTitle());
        return Result.success();
    }
}
