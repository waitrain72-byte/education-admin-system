package com.example.service;

import com.example.entity.Speciality;
import com.example.mapper.CrudMapper;
import com.example.mapper.SpecialityMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 专业信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class SpecialityService extends CrudService<Speciality> {

    @Resource
    private SpecialityMapper specialityMapper;

    @Override
    protected CrudMapper<Speciality> getMapper() {
        return specialityMapper;
    }
}
