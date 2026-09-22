package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.entity.Score;
import com.example.exception.CustomException;
import com.example.mapper.CourseMapper;
import com.example.mapper.CrudMapper;
import com.example.mapper.ScoreMapper;
import com.example.mapper.StudentMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** 及格线：达到该分数才计入学分 */
    private static final double PASS_LINE = 60;
    /** 总成绩权重：平时分 30% + 考试分 70% */
    private static final double WEIGHT_ORDINARY = 0.3;
    private static final double WEIGHT_EXAM = 0.7;

    /**
     * 新增：判断该学生该门课是否已录过成绩；计算总成绩；及格则给学生累加对应学分
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Score score) {
        Score dbScore = scoreMapper.selectByCourceIdAndStudentId(score.getCourseId(), score.getStudentId());
        if (ObjectUtil.isNotEmpty(dbScore)) {
            throw new CustomException(ResultCodeEnum.SCORE_ALREADY_ERROR);
        }
        double total = totalOf(score);
        score.setScore(total);
        scoreMapper.insert(score);
        // 录入之后，及格的学生需要获取对应的学分
        if (isPassed(total)) {
            adjustCredit(score.getStudentId(), score.getCourseId(), 1);
        }
    }

    /**
     * 修改：重算总成绩，并按「修改前是否及格 → 修改后是否及格」的状态迁移调整学分。
     *
     * <p>必须覆盖本方法：前端表单只提交平时分与考试分（不提交总成绩），
     * 若沿用 {@link CrudService#updateById} 的裸更新，总成绩会一直停留在录入时的旧值，
     * 且分数在及格线两侧变动时学分不会跟着调整。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(Score score) {
        Score dbScore = scoreMapper.selectById(score.getId());
        if (dbScore == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        // 平时分/考试分可能只传其中一个，缺省沿用库中原值后再重算总成绩
        if (score.getOrdinaryScore() == null) {
            score.setOrdinaryScore(dbScore.getOrdinaryScore());
        }
        if (score.getExamScore() == null) {
            score.setExamScore(dbScore.getExamScore());
        }
        double total = totalOf(score);
        score.setScore(total);
        scoreMapper.updateById(score);

        // 学分按及格状态的迁移增减：不及格→及格 加，及格→不及格 减，状态不变则不动
        boolean wasPassed = isPassed(dbScore.getScore());
        boolean nowPassed = isPassed(total);
        if (wasPassed != nowPassed) {
            Integer studentId = score.getStudentId() != null ? score.getStudentId() : dbScore.getStudentId();
            Integer courseId = score.getCourseId() != null ? score.getCourseId() : dbScore.getCourseId();
            adjustCredit(studentId, courseId, nowPassed ? 1 : -1);
        }
    }

    /**
     * 删除：仅当被删除的成绩原本及格时才扣减学分。
     * （新增时不及格是不加学分的，删除时若无条件扣减会把学生学分扣成负数）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer id) {
        Score score = scoreMapper.selectById(id);
        if (score == null) {
            return;
        }
        scoreMapper.deleteById(id);
        if (isPassed(score.getScore())) {
            adjustCredit(score.getStudentId(), score.getCourseId(), -1);
        }
    }

    /**
     * 批量删除：逐条走 {@link #deleteById} 以保证学分同步调整。
     * 不能沿用父类的单条 IN 语句——那会绕过学分扣减，导致学生学分虚高。
     */
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

    /** 总成绩 = 平时分 * 30% + 考试分 * 70% */
    private double totalOf(Score score) {
        if (score.getOrdinaryScore() == null || score.getExamScore() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        return score.getOrdinaryScore() * WEIGHT_ORDINARY + score.getExamScore() * WEIGHT_EXAM;
    }

    private boolean isPassed(Double total) {
        return total != null && total >= PASS_LINE;
    }

    /**
     * 按课程学分调整学生学分。sign 为 +1 累加、-1 扣减。
     * 走 SQL 原子自增，避免并发录入成绩时的丢更新。
     */
    private void adjustCredit(Integer studentId, Integer courseId, int sign) {
        if (studentId == null || courseId == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Integer credit = course.getScore();
        if (credit == null || credit == 0) {
            return;
        }
        studentMapper.addScore(studentId, sign * credit);
    }

    /** 成绩分段的档位码与展示文案（顺序即图表 X 轴顺序） */
    private static final String[][] SCORE_BUCKETS = {
            {"A", "优（90分-100分）"},
            {"B", "良（80分-89分）"},
            {"C", "中（70分-79分）"},
            {"D", "及格（60分-69分）"},
            {"E", "不及格（<60分）"},
    };

    /**
     * 成绩分段分布（首页折线图）：分桶交给数据库，按固定档位顺序补零返回。
     * 数据范围沿用 {@link #applyDataScope}，教师只统计本人任课、学生只统计本人。
     */
    public Map<String, List<Object>> scoreDistribution() {
        Score probe = new Score();
        applyDataScope(probe);

        Map<String, Long> countByBucket = new HashMap<>();
        for (Map<String, Object> row : scoreMapper.selectScoreDistribution(probe)) {
            Object bucket = row.get("bucket");
            Object value = row.get("value");
            countByBucket.put(String.valueOf(bucket), value instanceof Number ? ((Number) value).longValue() : 0L);
        }

        List<Object> xList = new ArrayList<>(SCORE_BUCKETS.length);
        List<Object> yList = new ArrayList<>(SCORE_BUCKETS.length);
        for (String[] bucket : SCORE_BUCKETS) {
            xList.add(bucket[1]);
            // 数据库只返回有数据的档位，缺失的档位补 0，否则折线图会缺点
            yList.add(countByBucket.getOrDefault(bucket[0], 0L));
        }
        Map<String, List<Object>> result = new HashMap<>();
        result.put("xAxis", xList);
        result.put("yAxis", yList);
        return result;
    }

    /**
     * 数据行级隔离：教师/学生只能查看自己的成绩（分页与全量接口统一生效）
     */
    @Override
    protected void applyDataScope(Score score) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
            score.setTeacherId(currentUser.getId());
        }
        if (RoleEnum.STUDENT.name().equals(currentUser.getRole())) {
            score.setStudentId(currentUser.getId());
        }
    }
}
