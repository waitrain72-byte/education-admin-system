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
     * 注册（仅学生开放注册）
     */
    public void register(Account account) {
        Student student = new Student();
        BeanUtils.copyProperties(account, student);
        add(student);
    }
}
