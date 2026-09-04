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

    /**
     * 字段白名单（本人自助更新）：电话/邮箱为教师本人可编辑资料（基类已放行），
     * 职称 title 由管理员维护，不允许教师本人通过自助接口修改。
     */
    @Override
    protected void sanitizeSelfUpdate(Teacher teacher) {
        super.sanitizeSelfUpdate(teacher);
        teacher.setTitle(null);
    }
}
