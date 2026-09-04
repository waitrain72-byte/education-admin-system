package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Comment;
import com.example.exception.CustomException;
import com.example.mapper.CommentMapper;
import com.example.mapper.CrudMapper;
import com.example.utils.TokenUtils;
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

    /**
     * 数据行级隔离：教师只能看到发给自己课程的评教；学生只能看到自己发起的评教；管理员可看全部
     * （分页与全量接口统一生效）
     */
    @Override
    protected void applyDataScope(Comment comment) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
            comment.setTeacher(currentUser.getName());
        }
        if (RoleEnum.STUDENT.name().equals(currentUser.getRole())) {
            comment.setStudent(currentUser.getName());
        }
    }
}
