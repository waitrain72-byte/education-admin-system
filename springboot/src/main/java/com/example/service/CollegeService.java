package com.example.service;

import com.example.entity.College;
import com.example.mapper.CollegeMapper;
import com.example.mapper.CrudMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 学院信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class CollegeService extends CrudService<College> {

    @Resource
    private CollegeMapper collegeMapper;
    @Resource
    private OrgService orgService;

    @Override
    protected CrudMapper<College> getMapper() {
        return collegeMapper;
    }

    /** 学院下面还有专业或学生时不许删（见 {@link OrgService#requireCollegeEmpty}） */
    @Override
    public void deleteById(Integer id) {
        orgService.requireCollegeEmpty(id);
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
