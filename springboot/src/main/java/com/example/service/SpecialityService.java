package com.example.service;

import com.example.entity.Speciality;
import com.example.mapper.CrudMapper;
import com.example.mapper.SpecialityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 专业信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class SpecialityService extends CrudService<Speciality> {

    @Resource
    private SpecialityMapper specialityMapper;
    @Resource
    private OrgService orgService;

    @Override
    protected CrudMapper<Speciality> getMapper() {
        return specialityMapper;
    }

    /** 专业下面还有班级或学生时不许删（见 {@link OrgService#requireSpecialityEmpty}） */
    @Override
    public void deleteById(Integer id) {
        orgService.requireSpecialityEmpty(id);
        super.deleteById(id);
    }

    /** 批量删除逐条检查 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Integer id : ids) {
            deleteById(id);
        }
    }
}
