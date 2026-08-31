package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.NoRepeatSubmit;
import com.example.entity.Comment;
import com.example.service.CommentService;
import com.example.service.CrudService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 评教信息表前端操作接口（通用增删改查见 {@link CrudController}）
 **/
@RestController
@RequestMapping("/comment")
public class CommentController extends CrudController<Comment> {

    @Resource
    private CommentService commentService;

    @Override
    protected CrudService<Comment> getService() {
        return commentService;
    }

    /**
     * 新增（防重复提交）
     */
    @Override
    @NoRepeatSubmit
    @PostMapping("/add")
    public Result add(@RequestBody Comment comment) {
        return super.add(comment);
    }
}
