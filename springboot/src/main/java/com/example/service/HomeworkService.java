package com.example.service;

import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.entity.Homework;
import com.example.entity.Teacher;
import com.example.mapper.CourseMapper;
import com.example.mapper.CrudMapper;
import com.example.mapper.HomeworkMapper;
import com.example.mapper.TeacherMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 作业信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class HomeworkService extends CrudService<Homework> {

    @Resource
    private HomeworkMapper homeworkMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private TeacherMapper teacherMapper;

    @Override
    protected CrudMapper<Homework> getMapper() {
        return homeworkMapper;
    }

    /**
     * 新增：根据课程找到任课教师，把作业归属给该教师
     */
    @Override
    public void add(Homework homework) {
        Course course = courseMapper.selectById(homework.getCourseId());
        Teacher teacher = teacherMapper.selectById(course.getTeacherId());
        homework.setTeacherId(teacher.getId());
        homeworkMapper.insert(homework);
    }

    /**
     * 分页查询（学生/教师只能查看自己的作业）
     */
    @Override
    public PageInfo<Homework> selectPage(Homework homework, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.STUDENT.name().equals(currentUser.getRole())) {
            homework.setStudentId(currentUser.getId());
        }
        if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
            homework.setTeacherId(currentUser.getId());
        }
        return super.selectPage(homework, pageNum, pageSize);
    }
}
