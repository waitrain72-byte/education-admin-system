package com.example.service;

import cn.hutool.core.date.DateUtil;
import com.example.entity.Examplan;
import com.example.mapper.CrudMapper;
import com.example.mapper.ExamplanMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 考试安排表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class ExamplanService extends CrudService<Examplan> {

    @Resource
    private ExamplanMapper examplanMapper;

    @Override
    protected CrudMapper<Examplan> getMapper() {
        return examplanMapper;
    }

    /**
     * 新增：生成发布时间
     */
    @Override
    public void add(Examplan examplan) {
        examplan.setTime(DateUtil.now());
        examplanMapper.insert(examplan);
    }
}
