package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Teacher;
import com.example.exception.CustomException;
import com.example.mapper.BaseMapper;
import com.example.mapper.PeopleMapper;
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
    @Resource
    private PeopleMapper peopleMapper;

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

    /** 还有课程（含已结课，成绩和评价都挂在上面）或带着班级的老师不能删 */
    @Override
    protected void beforeDelete(Integer id) {
        if (id != null && (peopleMapper.countCoursesOfTeacher(id) > 0 || peopleMapper.countClassesOfTeacher(id) > 0)) {
            throw new CustomException(ResultCodeEnum.TEACHER_IN_USE_ERROR);
        }
    }
}
