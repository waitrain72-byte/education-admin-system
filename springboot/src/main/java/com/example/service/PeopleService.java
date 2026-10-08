package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.exception.CustomException;
import com.example.mapper.PeopleMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 教务后台「人员」页：学生、教师、管理员三类账号放在一页里查（原来是三个独立的管理页）。
 * 新增、修改、删除、重置密码仍走 /student、/teacher、/admin 各自的接口（账号全局唯一等校验都在那边）。
 */
@Service
public class PeopleService {

    static final int MAX_PAGE_SIZE = 50;

    @Resource
    private PeopleMapper peopleMapper;

    public PageInfo<Map<String, Object>> students(String keyword, Integer collegeId, Integer specialityId, Integer classId,
                                                  Integer pageNum, Integer pageSize) {
        requireAdmin();
        startPage(pageNum, pageSize);
        return PageInfo.of(peopleMapper.students(normalize(keyword), collegeId, specialityId, classId));
    }

    public PageInfo<Map<String, Object>> teachers(String keyword, Integer pageNum, Integer pageSize) {
        requireAdmin();
        startPage(pageNum, pageSize);
        return PageInfo.of(peopleMapper.teachers(normalize(keyword)));
    }

    public PageInfo<Map<String, Object>> admins(String keyword, Integer pageNum, Integer pageSize) {
        requireAdmin();
        startPage(pageNum, pageSize);
        return PageInfo.of(peopleMapper.admins(normalize(keyword)));
    }

    private static void startPage(Integer pageNum, Integer pageSize) {
        int size = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
        PageHelper.startPage(pageNum == null ? 1 : Math.max(1, pageNum), size);
    }

    /** 关键字去掉首尾空格，最长 30 个字；空串当作不筛选 */
    static String normalize(String keyword) {
        String k = StrUtil.trimToNull(keyword);
        return k == null ? null : StrUtil.sub(k, 0, 30);
    }

    private static void requireAdmin() {
        if (!RoleEnum.ADMIN.name().equals(TokenUtils.getCurrentUser().getRole())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
    }
}
