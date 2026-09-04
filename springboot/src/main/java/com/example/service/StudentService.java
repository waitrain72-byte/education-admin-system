package com.example.service;

import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Student;
import com.example.mapper.BaseMapper;
import com.example.mapper.StudentMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 学生业务处理（通用逻辑见 BaseService）
 */
@Service
public class StudentService extends BaseService<Student> {

    @Resource
    private StudentMapper studentMapper;

    @Override
    protected BaseMapper<Student> getMapper() {
        return studentMapper;
    }

    @Override
    protected RoleEnum getRole() {
        return RoleEnum.STUDENT;
    }

    /**
     * 字段白名单（本人自助更新）：在基类（姓名/头像/密码/主题/语言）基础上，
     * 学分 score 由选课与成绩业务累加维护，学院/专业/班级由管理员维护，
     * 均不允许学生本人通过自助接口修改。
     */
    @Override
    protected void sanitizeSelfUpdate(Student student) {
        super.sanitizeSelfUpdate(student);
        student.setScore(null);
        student.setCollegeId(null);
        student.setSpecialityId(null);
        student.setClassId(null);
    }

    /**
     * 注册（仅学生开放注册）
     */
    public void register(Account account) {
        Student student = new Student();
        BeanUtils.copyProperties(account, student);
        add(student);
    }
}
