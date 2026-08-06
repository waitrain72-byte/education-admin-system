package com.example.service;

import com.example.common.enums.RoleEnum;
import com.example.entity.Admin;
import com.example.mapper.AdminMapper;
import com.example.mapper.BaseMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 管理员业务处理（通用逻辑见 BaseService）
 */
@Service
public class AdminService extends BaseService<Admin> {

    @Resource
    private AdminMapper adminMapper;

    @Override
    protected BaseMapper<Admin> getMapper() {
        return adminMapper;
    }

    @Override
    protected RoleEnum getRole() {
        return RoleEnum.ADMIN;
    }

    @Override
    protected boolean blockSelfReset() {
        // 管理员不能在列表中重置自己的密码，应通过“修改密码”页面修改
        return true;
    }
}
