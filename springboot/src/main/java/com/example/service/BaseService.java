package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.exception.CustomException;
import com.example.mapper.BaseMapper;
import com.example.utils.PasswordUtils;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import javax.annotation.Resource;
import java.util.List;

/**
 * 管理员/教师/学生三类账号的公共业务基类：
 * 统一实现登录、新增、改密、增删改查与分页，消除三个 Service 中重复的样板代码。
 * 子类只需指定对应的 Mapper 与角色。
 */
public abstract class BaseService<T extends Account> {

    @Resource
    protected FileService fileService;

    protected abstract BaseMapper<T> getMapper();

    protected abstract RoleEnum getRole();

    /** 是否禁止重置自己的密码（管理员子类覆盖为 true） */
    protected boolean blockSelfReset() {
        return false;
    }

    /**
     * 登录：校验账号与密码，兼容历史明文密码自动升级为 BCrypt，签发 JWT
     */
    public Account login(Account account) {
        T dbAccount = getMapper().selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbAccount)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbAccount.getPassword())) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        // 兼容历史明文密码：首次登录成功后自动升级为 BCrypt
        if (!PasswordUtils.isEncrypted(dbAccount.getPassword())) {
            dbAccount.setPassword(PasswordUtils.encrypt(account.getPassword()));
            getMapper().updateById(dbAccount);
        }
        // 生成 token
        String tokenData = dbAccount.getId() + "-" + getRole().name();
        String token = TokenUtils.createToken(tokenData);
        dbAccount.setToken(token);
        dbAccount.setPassword(null);
        return dbAccount;
    }

    /**
     * 新增：校验用户名唯一、默认密码/姓名、按角色设置 role
     */
    public void add(T entity) {
        T dbAccount = getMapper().selectByUsername(entity.getUsername());
        if (ObjectUtil.isNotNull(dbAccount)) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(entity.getPassword())) {
            entity.setPassword(Constants.USER_DEFAULT_PASSWORD);
        }
        entity.setPassword(PasswordUtils.encrypt(entity.getPassword()));
        if (ObjectUtil.isEmpty(entity.getName())) {
            entity.setName(entity.getUsername());
        }
        entity.setRole(getRole().name());
        getMapper().insert(entity);
    }

    /**
     * 修改密码
     */
    public void updatePassword(Account account) {
        T dbAccount = getMapper().selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbAccount)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbAccount.getPassword())) {
            throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        dbAccount.setPassword(PasswordUtils.encrypt(account.getNewPassword()));
        getMapper().updateById(dbAccount);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        getMapper().deleteById(id);
    }

    /**
     * 批量删除
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            getMapper().deleteById(id);
        }
    }

    /**
     * 修改：密码加密处理 + 头像引用清理。
     * 含行级保护：非管理员只能修改本人资料（对应 teacher:self / student:self / admin:self 权限），
     * 防止持有"本人资料"权限码的账号越权改动他人。
     */
    public void updateById(T entity) {
        // 行级数据权限：非管理员仅允许修改自己；管理员不受限（无 ID 说明是新增场景，不拦截）
        if (entity.getId() != null) {
            Account currentUser = TokenUtils.getCurrentUser();
            if (!RoleEnum.ADMIN.name().equals(currentUser.getRole())
                    && currentUser.getId() != null
                    && !currentUser.getId().equals(entity.getId())) {
                throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
            }
        }
        T oldEntity = entity.getId() == null ? null : getMapper().selectById(entity.getId());
        if (ObjectUtil.isNotEmpty(entity.getPassword()) && !PasswordUtils.isEncrypted(entity.getPassword())) {
            entity.setPassword(PasswordUtils.encrypt(entity.getPassword()));
        }
        getMapper().updateById(entity);
        if (oldEntity != null && ObjectUtil.isNotEmpty(entity.getAvatar())) {
            fileService.deleteAvatarIfUnused(oldEntity.getAvatar(), entity.getAvatar());
        }
    }

    /**
     * 根据 ID 查询
     */
    public T selectById(Integer id) {
        return getMapper().selectById(id);
    }

    /**
     * 查询所有
     */
    public List<T> selectAll(T entity) {
        return getMapper().selectAll(entity);
    }

    /**
     * 分页查询
     */
    public PageInfo<T> selectPage(T entity, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<T> list = getMapper().selectAll(entity);
        return PageInfo.of(list);
    }

    /**
     * 更新界面语言：取值合法性（zh-CN/en-US）由调用方白名单校验，
     * 这里回读当前数据后仅覆盖 locale 字段，避免误改其他资料。
     */
    public void updateLocale(Account account) {
        T dbAccount = getMapper().selectById(account.getId());
        if (ObjectUtil.isNull(dbAccount)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        dbAccount.setLocale(account.getLocale());
        getMapper().updateById(dbAccount);
    }

    /**
     * 更新主题偏好：取值合法性（light/dark/system）由调用方白名单校验，
     * 这里回读当前数据后仅覆盖 theme 字段，避免误改其他资料。
     */
    public void updateTheme(Account account) {
        T dbAccount = getMapper().selectById(account.getId());
        if (ObjectUtil.isNull(dbAccount)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        dbAccount.setTheme(account.getTheme());
        getMapper().updateById(dbAccount);
    }

    /**
     * 管理员重置密码（仅管理员可调用）
     */
    public void resetPassword(Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (!RoleEnum.ADMIN.name().equals(currentUser.getRole())) {
            throw new CustomException("403", "Only admins can reset passwords");
        }
        if (blockSelfReset() && currentUser.getId() != null && currentUser.getId().equals(id)) {
            throw new CustomException("400", "Cannot reset current admin password");
        }
        T dbAccount = getMapper().selectById(id);
        if (ObjectUtil.isNull(dbAccount)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        dbAccount.setPassword(PasswordUtils.encrypt(Constants.USER_DEFAULT_PASSWORD));
        getMapper().updateById(dbAccount);
    }
}
