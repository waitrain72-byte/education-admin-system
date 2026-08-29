package com.example.service;

import com.example.entity.LoginLog;
import com.example.entity.OperLog;
import com.example.mapper.LoginLogMapper;
import com.example.mapper.OperLogMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 日志服务：操作日志与登录日志的写入与分页查询。
 * 写入失败时静默忽略——日志不应影响业务主流程。
 */
@Service
public class LogService {

    @Resource
    private OperLogMapper operLogMapper;
    @Resource
    private LoginLogMapper loginLogMapper;

    public void insertOper(OperLog log) {
        try {
            operLogMapper.insert(log);
        } catch (Exception ignored) {
        }
    }

    public void insertLogin(LoginLog log) {
        try {
            loginLogMapper.insert(log);
        } catch (Exception ignored) {
        }
    }

    public PageInfo<OperLog> selectOperPage(OperLog query, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return PageInfo.of(operLogMapper.selectAll(query));
    }

    public PageInfo<LoginLog> selectLoginPage(LoginLog query, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return PageInfo.of(loginLogMapper.selectAll(query));
    }

    public void deleteOperById(Integer id) {
        operLogMapper.deleteById(id);
    }

    public void deleteOperBatch(java.util.List<Integer> ids) {
        operLogMapper.deleteBatch(ids);
    }

    public void deleteLoginById(Integer id) {
        loginLogMapper.deleteById(id);
    }

    public void deleteLoginBatch(java.util.List<Integer> ids) {
        loginLogMapper.deleteBatch(ids);
    }
}
