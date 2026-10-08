package com.example.service;

import com.example.entity.Classes;
import com.example.mapper.ClassesMapper;
import com.example.mapper.CrudMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 班级信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class ClassesService extends CrudService<Classes> {

    @Resource
    private ClassesMapper classesMapper;
    @Resource
    private OrgService orgService;

    @Override
    protected CrudMapper<Classes> getMapper() {
        return classesMapper;
    }

    /** 班级里还有学生时不许删（见 {@link OrgService#requireClassEmpty}） */
    @Override
    public void deleteById(Integer id) {
        orgService.requireClassEmpty(id);
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
