package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Choice;
import com.example.exception.CustomException;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CrudMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 选课信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class ChoiceService extends CrudService<Choice> {

    @Resource
    private ChoiceMapper choiceMapper;
    @Resource
    private EnrollmentService enrollmentService;

    @Override
    protected CrudMapper<Choice> getMapper() {
        return choiceMapper;
    }

    /**
     * 新增（选课）：规则见 {@link EnrollmentService#enroll}。学生只能给自己选；管理员可以替学生选。
     */
    @Override
    public void add(Choice choice) {
        Account current = TokenUtils.getCurrentUser();
        Integer studentId = RoleEnum.STUDENT.name().equals(current.getRole()) ? current.getId() : choice.getStudentId();
        if (!RoleEnum.STUDENT.name().equals(current.getRole()) && !RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        enrollmentService.enroll(studentId, choice.getCourseId());
    }

    /**
     * 删除（退选）：规则见 {@link EnrollmentService#drop}。学生只能退自己的课；管理员可以替学生退。
     */
    @Override
    public void deleteById(Integer id) {
        Choice row = id == null ? null : choiceMapper.selectById(id);
        if (row == null) {
            return;
        }
        Account current = TokenUtils.getCurrentUser();
        boolean student = RoleEnum.STUDENT.name().equals(current.getRole());
        if (student && !current.getId().equals(row.getStudentId())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        if (!student && !RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        enrollmentService.drop(row.getStudentId(), row.getCourseId());
    }

    /** 批量退选：逐条走 {@link #deleteById}，每一条都做归属与规则检查 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    /**
     * 数据行级隔离：教师/学生只能查看自己的选课（分页与全量接口统一生效）
     */
    @Override
    protected void applyDataScope(Choice choice) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
            choice.setTeacherId(currentUser.getId());
        }
        if (RoleEnum.STUDENT.name().equals(currentUser.getRole())) {
            choice.setStudentId(currentUser.getId());
        }
    }
}
