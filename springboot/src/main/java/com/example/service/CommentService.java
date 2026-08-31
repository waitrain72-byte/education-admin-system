package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Comment;
import com.example.exception.CustomException;
import com.example.mapper.CommentMapper;
import com.example.mapper.CrudMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 评教信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class CommentService extends CrudService<Comment> {

    @Resource
    private CommentMapper commentMapper;

    @Override
    protected CrudMapper<Comment> getMapper() {
        return commentMapper;
    }

    /**
     * 新增：判断该学生对该门课是否已经评教过
     */
    @Override
    public void add(Comment comment) {
        Comment dbComment = commentMapper.selectByTeacherAndCourseAndStudent(comment.getTeacher(), comment.getName(), comment.getStudent());
        if (ObjectUtil.isNotEmpty(dbComment)) {
            throw new CustomException(ResultCodeEnum.COMMENT_ALREADY_ERROR);
        }
        comment.setTime(DateUtil.now());
        commentMapper.insert(comment);
    }
}
