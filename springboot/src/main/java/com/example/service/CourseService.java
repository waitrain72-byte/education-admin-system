package com.example.service;

import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.mapper.CourseMapper;
import com.example.mapper.CrudMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageInfo;
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
     * 分页查询（教师只能查看自己开设的课程）
     */
    @Override
    public PageInfo<Course> selectPage(Course course, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
            course.setTeacherId(currentUser.getId());
        }
        return super.selectPage(course, pageNum, pageSize);
    }
}
