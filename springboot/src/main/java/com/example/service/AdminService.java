package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Admin;
import com.example.exception.CustomException;
import com.example.mapper.AdminMapper;
import com.example.utils.PasswordUtils;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 管理员业务处理
 **/
@Service
public class AdminService {

    @Resource
    private AdminMapper adminMapper;
    @Resource
    private FileService fileService;

    /**
     * 新增
     */
    public void add(Admin admin) {
        Admin dbAdmin = adminMapper.selectByUsername(admin.getUsername());
        if (ObjectUtil.isNotNull(dbAdmin)) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(admin.getPassword())) {
            admin.setPassword(Constants.USER_DEFAULT_PASSWORD);/*没设置密码就默认*/
        }
        admin.setPassword(PasswordUtils.encrypt(admin.getPassword()));
        if (ObjectUtil.isEmpty(admin.getName())) {
            admin.setName(admin.getUsername());
        }
        admin.setRole(RoleEnum.ADMIN.name());
        adminMapper.insert(admin);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        adminMapper.deleteById(id);
    }

    /**
     * 批量删除
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            adminMapper.deleteById(id);
        }
    }

    /**
     * 修改
     */
    public void updateById(Admin admin) {
        Admin oldAdmin = admin.getId() == null ? null : adminMapper.selectById(admin.getId());
        if (ObjectUtil.isNotEmpty(admin.getPassword()) && !PasswordUtils.isEncrypted(admin.getPassword())) {
            admin.setPassword(PasswordUtils.encrypt(admin.getPassword()));
        }
        adminMapper.updateById(admin);
        if (oldAdmin != null && ObjectUtil.isNotEmpty(admin.getAvatar())) {
            fileService.deleteAvatarIfUnused(oldAdmin.getAvatar(), admin.getAvatar());
        }
    }

    /**
     * 根据ID查询
     */
    public Admin selectById(Integer id) {
        return adminMapper.selectById(id);
    }

    /**
     * 查询所有
     */
    public List<Admin> selectAll(Admin admin) {
        return adminMapper.selectAll(admin);
    }

    /**
     * 分页查询
     */
    public PageInfo<Admin> selectPage(Admin admin, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Admin> list = adminMapper.selectAll(admin);
        return PageInfo.of(list);
    }

    /**
     * 登录
     */
    public Account login(Account account) {
        Admin dbAdmin = adminMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbAdmin)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbAdmin.getPassword())) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        if (!PasswordUtils.isEncrypted(dbAdmin.getPassword())) {
            dbAdmin.setPassword(PasswordUtils.encrypt(account.getPassword()));
            adminMapper.updateById(dbAdmin);
        }
        // 生成token
        String tokenData = dbAdmin.getId() + "-" + RoleEnum.ADMIN.name();
        String token = TokenUtils.createToken(tokenData, dbAdmin.getPassword());
        dbAdmin.setToken(token);
        dbAdmin.setPassword(null);
        return dbAdmin;
    }

//    /**
//     * 注册
//     */
//    public void register(Account account) {
//        Admin admin = new Admin();
//        BeanUtils.copyProperties(account, admin);
//        add(admin);
//    }

    /**
     * 修改密码
     */
    public void updatePassword(Account account) {
        Admin dbAdmin = adminMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbAdmin)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbAdmin.getPassword())) {
            throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        dbAdmin.setPassword(PasswordUtils.encrypt(account.getNewPassword()));
        adminMapper.updateById(dbAdmin);
    }

    public void resetPassword(Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (!RoleEnum.ADMIN.name().equals(currentUser.getRole())) {
            throw new CustomException("403", "Only admins can reset passwords");
        }
        if (currentUser.getId().equals(id)) {
            throw new CustomException("400", "Cannot reset current admin password");
        }
        Admin dbAdmin = adminMapper.selectById(id);
        if (ObjectUtil.isNull(dbAdmin)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        dbAdmin.setPassword(PasswordUtils.encrypt(Constants.USER_DEFAULT_PASSWORD));
        adminMapper.updateById(dbAdmin);
    }

}
