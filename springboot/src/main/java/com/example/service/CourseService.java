package com.example.service;

import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.mapper.CourseMapper;
import com.example.mapper.CrudMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 课程信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class CourseService extends CrudService<Course> {

    @Resource
    private CourseMapper courseMapper;

    @Override
    protected CrudMapper<Course> getMapper() {
        return courseMapper;
    }

    /**
     * 数据行级隔离：教师只能查看自己开设的课程（分页与全量接口统一生效）
     */
    @Override
    protected void applyDataScope(Course course) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
            course.setTeacherId(currentUser.getId());
        }
    }
}
