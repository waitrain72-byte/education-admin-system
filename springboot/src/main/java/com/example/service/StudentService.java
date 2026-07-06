package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Student;
import com.example.exception.CustomException;
import com.example.mapper.StudentMapper;
import com.example.utils.PasswordUtils;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 学生业务处理
 **/
@Service
public class StudentService {

    @Resource
    private StudentMapper studentMapper;
    @Resource
    private FileService fileService;
    /**
     * 登录
     */
    public Account login(Account account) {
        Student dbStudent = studentMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbStudent)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbStudent.getPassword())) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_ERROR);
        }
        if (!PasswordUtils.isEncrypted(dbStudent.getPassword())) {
            dbStudent.setPassword(PasswordUtils.encrypt(account.getPassword()));
            studentMapper.updateById(dbStudent);
        }
        // 生成token
        String tokenData = dbStudent.getId() + "-" + RoleEnum.STUDENT.name();
        String token = TokenUtils.createToken(tokenData, dbStudent.getPassword());
        dbStudent.setToken(token);
        dbStudent.setPassword(null);
        return dbStudent;
    }

    /**
     * 注册
     */
    public void register(Account account) {
        Student student = new Student();
        BeanUtils.copyProperties(account, student);
        add(student);
    }

    /**
     * 新增
     */
    public void add(Student student) {
        Student dbStudent = studentMapper.selectByUsername(student.getUsername());
        if (ObjectUtil.isNotNull(dbStudent)) {
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if (ObjectUtil.isEmpty(student.getPassword())) {
            student.setPassword(Constants.USER_DEFAULT_PASSWORD);
        }
        student.setPassword(PasswordUtils.encrypt(student.getPassword()));
        if (ObjectUtil.isEmpty(student.getName())) {
            student.setName(student.getUsername());
        }
        student.setRole(RoleEnum.STUDENT.name());
        studentMapper.insert(student);              // 这里设置了role为STUDENT
    }
    /**
     * 修改密码
     */
    public void updatePassword(Account account) {
        Student dbStudent = studentMapper.selectByUsername(account.getUsername());
        if (ObjectUtil.isNull(dbStudent)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if (!PasswordUtils.matches(account.getPassword(), dbStudent.getPassword())) {
            throw new CustomException(ResultCodeEnum.PARAM_PASSWORD_ERROR);
        }
        dbStudent.setPassword(PasswordUtils.encrypt(account.getNewPassword()));
        studentMapper.updateById(dbStudent);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        studentMapper.deleteById(id);
    }

    /**
     * 批量删除
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            studentMapper.deleteById(id);
        }
    }

    /**
     * 修改
     */
    public void updateById(Student student) {
        Student oldStudent = student.getId() == null ? null : studentMapper.selectById(student.getId());
        if (ObjectUtil.isNotEmpty(student.getPassword()) && !PasswordUtils.isEncrypted(student.getPassword())) {
            student.setPassword(PasswordUtils.encrypt(student.getPassword()));
        }
        studentMapper.updateById(student);
        if (oldStudent != null && ObjectUtil.isNotEmpty(student.getAvatar())) {
            fileService.deleteAvatarIfUnused(oldStudent.getAvatar(), student.getAvatar());
        }
    }

    /**
     * 根据ID查询
     */
    public Student selectById(Integer id) {
        return studentMapper.selectById(id);
    }

    public void resetPassword(Integer id) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (!RoleEnum.ADMIN.name().equals(currentUser.getRole())) {
            throw new CustomException("403", "Only admins can reset passwords");
        }
        Student dbStudent = studentMapper.selectById(id);
        if (ObjectUtil.isNull(dbStudent)) {
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        dbStudent.setPassword(PasswordUtils.encrypt(Constants.USER_DEFAULT_PASSWORD));
        studentMapper.updateById(dbStudent);
    }

    /**
     * 查询所有
     */
    public List<Student> selectAll(Student student) {
        return studentMapper.selectAll(student);
    }

    /**
     * 分页查询
     */
    public PageInfo<Student> selectPage(Student student, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Student> list = studentMapper.selectAll(student);
        return PageInfo.of(list);
    }
}
