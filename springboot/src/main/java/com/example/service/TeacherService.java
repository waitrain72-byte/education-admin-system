package com.example.service;

import com.example.common.enums.RoleEnum;
import com.example.entity.Teacher;
import com.example.mapper.BaseMapper;
import com.example.mapper.TeacherMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 教师业务处理（通用逻辑见 BaseService）
 */
@Service
public class TeacherService extends BaseService<Teacher> {

    @Resource
    private TeacherMapper teacherMapper;

    @Override
    protected BaseMapper<Teacher> getMapper() {
        return teacherMapper;
    }

    @Override
    protected RoleEnum getRole() {
        return RoleEnum.TEACHER;
    }
}
