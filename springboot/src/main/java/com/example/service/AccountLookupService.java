package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.exception.CustomException;
import com.example.mapper.AdminMapper;
import com.example.mapper.StudentMapper;
import com.example.mapper.TeacherMapper;
import com.example.utils.PasswordUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 跨三张账号表（管理员 / 教师 / 学生）的用户名查询。
 *
 * <p>登录页不再让用户选身份，后端按用户名判断是谁：只有一张表里有这个用户名就直接用它；
 * 好几张表里都有（老数据里可能出现）时按密码区分，仍然分不出来才要求前端补选身份。
 * 新建、修改账号时则保证用户名在三张表之间全局唯一，从源头避免出现这种情况。</p>
 */
@Service
public class AccountLookupService {

    @Resource
    private AdminMapper adminMapper;
    @Resource
    private TeacherMapper teacherMapper;
    @Resource
    private StudentMapper studentMapper;

    /**
     * 按用户名与密码判断登录身份。
     *
     * @return 角色名（ADMIN / TEACHER / STUDENT）
     * @throws CustomException 5004 账号不存在；5003 多个同名账号但密码都不对；5012 多个同名账号密码也相同，需要用户选择身份
     */
    public String resolveRole(String username, String password) {
        Map<RoleEnum, Account> found = findByUsername(username);
        if (found.isEmpty()) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (found.size() == 1) {
            // 唯一匹配：密码对不对交给正常登录流程判断，失败计数、锁定、登录日志与选身份登录完全一致
            return found.keySet().iterator().next().name();
        }
        List<RoleEnum> matched = new ArrayList<>();
        for (Map.Entry<RoleEnum, Account> entry : found.entrySet()) {
            if (PasswordUtils.matches(password, entry.getValue().getPassword())) {
                matched.add(entry.getKey());
            }
        }
        if (matched.size() == 1) {
            return matched.get(0).name();
        }
        if (matched.isEmpty()) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        throw new CustomException(ResultCodeEnum.ROLE_REQUIRED_ERROR);
    }

    /**
     * 用户名是否已被占用（三张表一起查）。
     *
     * @param excludeRole 修改账号时传自己的角色，与 excludeId 一起把自己排除在外；新增时传 null
     */
    public boolean isUsernameTaken(String username, RoleEnum excludeRole, Integer excludeId) {
        if (StrUtil.isBlank(username)) {
            return false;
        }
        for (Map.Entry<RoleEnum, Account> entry : findByUsername(username).entrySet()) {
            boolean isSelf = entry.getKey() == excludeRole && excludeId != null
                    && excludeId.equals(entry.getValue().getId());
            if (!isSelf) {
                return true;
            }
        }
        return false;
    }

    /** 三张表里同名的账号，顺序固定为 管理员 → 教师 → 学生 */
    Map<RoleEnum, Account> findByUsername(String username) {
        Map<RoleEnum, Account> found = new LinkedHashMap<>();
        if (StrUtil.isBlank(username)) {
            return found;
        }
        Account admin = adminMapper.selectByUsername(username);
        if (admin != null) {
            found.put(RoleEnum.ADMIN, admin);
        }
        Account teacher = teacherMapper.selectByUsername(username);
        if (teacher != null) {
            found.put(RoleEnum.TEACHER, teacher);
        }
        Account student = studentMapper.selectByUsername(username);
        if (student != null) {
            found.put(RoleEnum.STUDENT, student);
        }
        return found;
    }
}
