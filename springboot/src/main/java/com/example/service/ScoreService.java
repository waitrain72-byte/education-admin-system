package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.entity.Score;
import com.example.entity.Student;
import com.example.exception.CustomException;
import com.example.mapper.CourseMapper;
import com.example.mapper.CrudMapper;
import com.example.mapper.ScoreMapper;
import com.example.mapper.StudentMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 学分信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class ScoreService extends CrudService<Score> {

    @Resource
    private ScoreMapper scoreMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private StudentMapper studentMapper;

    @Override
    protected CrudMapper<Score> getMapper() {
        return scoreMapper;
    }

    /**
     * 新增：判断该学生该门课是否已录过成绩；计算总成绩；及格则给学生累加对应学分
     */
    @Override
    public void add(Score score) {
        Score dbScore = scoreMapper.selectByCourceIdAndStudentId(score.getCourseId(), score.getStudentId());
        if (ObjectUtil.isNotEmpty(dbScore)) {
            throw new CustomException(ResultCodeEnum.SCORE_ALREADY_ERROR);
        }
        double total = score.getOrdinaryScore() * 0.3 + score.getExamScore() * 0.7;
        score.setScore(total);
        scoreMapper.insert(score);
        // 录入之后，及格的学生需要获取对应的学分
        if (total >= 60) {
            Course course = courseMapper.selectById(score.getCourseId());
            Student student = studentMapper.selectById(score.getStudentId());
            student.setScore(student.getScore() + course.getScore());
            studentMapper.updateById(student);
        }
    }

    /**
     * 删除：删除成绩的同时扣除学生对应的学分
     */
    @Override
    public void deleteById(Integer id) {
        Score score = scoreMapper.selectById(id);
        scoreMapper.deleteById(id);
        Student student = studentMapper.selectById(score.getStudentId());
        Course course = courseMapper.selectById(score.getCourseId());
        student.setScore(student.getScore() - course.getScore());
        studentMapper.updateById(student);
    }

    /**
     * 分页查询（教师/学生只能查看自己的成绩）
     */
    @Override
    public PageInfo<Score> selectPage(Score score, Integer pageNum, Integer pageSize) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
            score.setTeacherId(currentUser.getId());
        }
        if (RoleEnum.STUDENT.name().equals(currentUser.getRole())) {
            score.setStudentId(currentUser.getId());
        }
        return super.selectPage(score, pageNum, pageSize);
    }
}
