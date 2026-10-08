package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Choice;
import com.example.entity.Course;
import com.example.entity.Score;
import com.example.exception.CustomException;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.ScoreMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 选课与退选的规则（课程广场、旧版选课页、小程序都走这里）。
 *
 * <ul>
 *   <li>选课：课程存在且没结课、没选过、没满员（带行锁计数，防并发超额）、和已选的未结课课程不在同一时段；</li>
 *   <li>退选：选了这门课、还没结课、成绩没发布；退选时顺手清掉成绩册里这名学生的草稿成绩。</li>
 * </ul>
 */
@Service
public class EnrollmentService {

    static final String FINISHED = "已结课";

    @Resource
    private CourseMapper courseMapper;
    @Resource
    private ChoiceMapper choiceMapper;
    @Resource
    private ScoreMapper scoreMapper;

    @Transactional(rollbackFor = Exception.class)
    public Course enroll(Integer studentId, Integer courseId) {
        if (studentId == null || courseId == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        // 带行锁读取课程，锁住该课程行直到事务提交，避免并发选课同时读到「未满」而超额
        Course course = courseMapper.selectByIdForUpdate(courseId);
        if (course == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        if (FINISHED.equals(course.getStatus())) {
            throw new CustomException(ResultCodeEnum.COURSE_ENDED_ERROR);
        }
        if (choiceMapper.countByStudentAndCourse(studentId, courseId) > 0) {
            throw new CustomException(ResultCodeEnum.ALREADY_ENROLLED_ERROR);
        }
        // 用 >= 而不是相等判断：历史数据一旦超过上限也照样拦住
        if (course.getNum() != null && choiceMapper.countByCourseId(courseId) >= course.getNum()) {
            throw new CustomException(ResultCodeEnum.COURSE_NUM_ERROR);
        }
        Course clash = conflictOf(course, choiceMapper.selectActiveSlotsByStudentId(studentId));
        if (clash != null) {
            throw new CustomException(ResultCodeEnum.SCHEDULE_CONFLICT_ERROR.code,
                    "和已选的《" + clash.getName() + "》上课时间冲突");
        }
        Choice choice = new Choice();
        choice.setStudentId(studentId);
        choice.setCourseId(courseId);
        choice.setTeacherId(course.getTeacherId());
        choiceMapper.insert(choice);
        return course;
    }

    @Transactional(rollbackFor = Exception.class)
    public void drop(Integer studentId, Integer courseId) {
        if (studentId == null || courseId == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null || choiceMapper.countByStudentAndCourse(studentId, courseId) == 0) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Score score = scoreMapper.selectByCourceIdAndStudentId(courseId, studentId);
        if (FINISHED.equals(course.getStatus()) || (score != null && GradebookService.isPublished(score))) {
            throw new CustomException(ResultCodeEnum.DROP_LOCKED_ERROR);
        }
        if (score != null) {
            scoreMapper.deleteById(score.getId());
        }
        choiceMapper.deleteByStudentAndCourse(studentId, courseId);
    }

    /** 退选前的检查：能不能退（课程广场、课程概览用来决定要不要显示「退选」） */
    public boolean canDrop(Integer studentId, Course course) {
        if (studentId == null || course == null || FINISHED.equals(course.getStatus())) {
            return false;
        }
        Score score = scoreMapper.selectByCourceIdAndStudentId(course.getId(), studentId);
        return score == null || !GradebookService.isPublished(score);
    }

    /** 和已选课程里同一「星期 + 大节」的那门课；没排课的课程不算冲突 */
    static Course conflictOf(Course course, List<Course> selected) {
        if (course == null || course.getWeek() == null || course.getSegment() == null) {
            return null;
        }
        for (Course other : selected) {
            if (other.getId() != null && other.getId().equals(course.getId())) {
                continue;
            }
            if (course.getWeek().equals(other.getWeek()) && course.getSegment().equals(other.getSegment())) {
                return other;
            }
        }
        return null;
    }
}
