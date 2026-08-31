package com.example.service;

import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Apply;
import com.example.mapper.ApplyMapper;
import com.example.mapper.CrudMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 请假信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class ApplyService extends CrudService<Apply> {

    @Resource
    private ApplyMapper applyMapper;

    @Override
    protected CrudMapper<Apply> getMapper() {
        return applyMapper;
    }

    /**
     * 分页查询（用 jwt 解析 token 进行角色判定，学生只能看自己的请假记录）
     */
    @Override
    public PageInfo<Apply> selectPage(Apply apply, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.STUDENT.name().equals(currentUser.getRole())) {
            apply.setStudentId(currentUser.getId());
        }
        return super.selectPage(apply, pageNum, pageSize);
    }
}
