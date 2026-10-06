package com.example.controller;

import com.example.common.Result;
import com.example.service.MessageService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 消息中心接口：只读写当前登录人自己的消息，登录即可调用，不需要额外权限码
 */
@RestController
@RequestMapping("/message")
public class MessageController {

    @Resource
    private MessageService messageService;

    @GetMapping("/page")
    public Result page(@RequestParam(defaultValue = "false") boolean unreadOnly,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(messageService.page(unreadOnly, pageNum, pageSize));
    }

    @GetMapping("/unreadCount")
    public Result unreadCount() {
        return Result.success(messageService.unreadCount());
    }

    @PutMapping("/read/{id}")
    public Result read(@PathVariable Integer id) {
        messageService.markRead(id);
        return Result.success();
    }

    @PutMapping("/readAll")
    public Result readAll() {
        messageService.markAllRead();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        messageService.delete(id);
        return Result.success();
    }
}
