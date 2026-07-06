package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Teacher;
import com.example.exception.CustomException;
import com.example.mapper.TeacherMapper;
import com.example.utils.PasswordUtils;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 教师业务处理
 **/
@Service
public class TeacherService {

    @Resource
    private TeacherMapper teacherMapper;
    @Resource
    private FileService fileService;
    /**
     * 登录
     */
    public Account login(Account account) {
        Teacher dbTeacher = teacherMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbTeacher)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbTeacher.getPassword())) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        if (!PasswordUtils.isEncrypted(dbTeacher.getPassword())) {
            dbTeacher.setPassword(PasswordUtils.encrypt(account.getPassword()));
            teacherMapper.updateById(dbTeacher);
        }
        // 生成token
        String tokenData = dbTeacher.getId() + "-" + RoleEnum.TEACHER.name();
        String token = TokenUtils.createToken(tokenData, dbTeacher.getPassword());
        dbTeacher.setToken(token);
        dbTeacher.setPassword(null);
        return dbTeacher;
    }
    /**
     * 新增
     */
    public void add(Teacher teacher) {
        Teacher dbTeacher = teacherMapper.selectByUsername(teacher.getUsername());
        if (ObjectUtil.isNotNull(dbTeacher)) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(teacher.getPassword())) {
            teacher.setPassword(Constants.USER_DEFAULT_PASSWORD);
        }
        teacher.setPassword(PasswordUtils.encrypt(teacher.getPassword()));
        if (ObjectUtil.isEmpty(teacher.getName())) {
            teacher.setName(teacher.getUsername());
        }
        teacher.setRole(RoleEnum.TEACHER.name());
        teacherMapper.insert(teacher);              // 这里设置了role为TEACHER
    }
    /**
     * 修改密码
     */
    public void updatePassword(Account account) {
        Teacher dbTeacher = teacherMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbTeacher)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbTeacher.getPassword())) {
            throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        dbTeacher.setPassword(PasswordUtils.encrypt(account.getNewPassword()));
        teacherMapper.updateById(dbTeacher);
    }
    /**
     * 删除
     */
    public void deleteById(Integer id) {
        teacherMapper.deleteById(id);
    }

    /**
     * 批量删除
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            teacherMapper.deleteById(id);
        }
    }

    /**
     * 修改
     */
    public void updateById(Teacher teacher) {
        Teacher oldTeacher = teacher.getId() == null ? null : teacherMapper.selectById(teacher.getId());
        if (ObjectUtil.isNotEmpty(teacher.getPassword()) && !PasswordUtils.isEncrypted(teacher.getPassword())) {
            teacher.setPassword(PasswordUtils.encrypt(teacher.getPassword()));
        }
        teacherMapper.updateById(teacher);
        if (oldTeacher != null && ObjectUtil.isNotEmpty(teacher.getAvatar())) {
            fileService.deleteAvatarIfUnused(oldTeacher.getAvatar(), teacher.getAvatar());
        }
    }

    /**
     * 根据ID查询
     */
    public Teacher selectById(Integer id) {
        return teacherMapper.selectById(id);
    }

    public void resetPassword(Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (!RoleEnum.ADMIN.name().equals(currentUser.getRole())) {
            throw new CustomException("403", "Only admins can reset passwords");
        }
        Teacher dbTeacher = teacherMapper.selectById(id);
        if (ObjectUtil.isNull(dbTeacher)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        dbTeacher.setPassword(PasswordUtils.encrypt(Constants.USER_DEFAULT_PASSWORD));
        teacherMapper.updateById(dbTeacher);
    }

    /**
     * 查询所有
     */
    public List<Teacher> selectAll(Teacher teacher) {
        return teacherMapper.selectAll(teacher);
    }

    /**
     * 分页查询
     */
    public PageInfo<Teacher> selectPage(Teacher teacher, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Teacher> list = teacherMapper.selectAll(teacher);
        return PageInfo.of(list);
    }

}
