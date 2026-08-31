package com.example.service;

import com.example.entity.College;
import com.example.mapper.CollegeMapper;
import com.example.mapper.CrudMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 学院信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class CollegeService extends CrudService<College> {

    @Resource
    private CollegeMapper collegeMapper;

    @Override
    protected CrudMapper<College> getMapper() {
        return collegeMapper;
    }
}
