package com.example.service;

import com.example.entity.Classes;
import com.example.mapper.ClassesMapper;
import com.example.mapper.CrudMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 班级信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class ClassesService extends CrudService<Classes> {

    @Resource
    private ClassesMapper classesMapper;

    @Override
    protected CrudMapper<Classes> getMapper() {
        return classesMapper;
    }
}
