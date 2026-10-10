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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 成绩表的通用增删改查（见 {@link CrudService}），教务后台的旧版成绩页在用；课程空间里的成绩册见 {@link GradebookService}。
 *
 * <p>总评按课程设置的四项权重计算（{@link GradeCalculator}）。旧客户端只录平时分和期末分：
 * 课程给考勤、作业设了权重时，这两项按记录自动折算后补上。旧接口录入的成绩直接是「已发布」，
 * 学分只按「已发布且及格」的成绩累计。</p>
 */
@Service
public class ScoreService extends CrudService<Score> {

    @Resource
    private ScoreMapper scoreMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private StudentMapper studentMapper;
    @Resource
    private GradebookService gradebookService;

    @Override
    protected CrudMapper<Score> getMapper() {
        return scoreMapper;
    }

    /**
     * 新增：判断该学生该门课是否已录过成绩；按课程权重计算总评；直接发布，及格则给学生累加对应学分
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Score score) {
        Score dbScore = scoreMapper.selectByCourceIdAndStudentId(score.getCourseId(), score.getStudentId());
        if (ObjectUtil.isNotEmpty(dbScore)) {
            throw new CustomException(ResultCodeEnum.SCORE_ALREADY_ERROR);
        }
        Course course = score.getCourseId() == null ? null : courseMapper.selectById(score.getCourseId());
        if (course == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        requireComponents(score);
        score.setScore(totalOf(course, score));
        score.setStatus(GradeCalculator.PUBLISHED);
        scoreMapper.insert(score);
        // 录入之后，及格的学生需要获取对应的学分
        if (counts(score.getStatus(), score.getScore())) {
            adjustCredit(score.getStudentId(), course, 1);
        }
    }

    /**
     * 修改：重算总成绩，并按「修改前是否计入学分 → 修改后是否计入学分」的状态迁移调整学分。
     *
     * <p>必须覆盖本方法：前端表单只提交平时分与考试分（不提交总成绩），
     * 若沿用 {@link CrudService#updateById} 的裸更新，总成绩会一直停留在录入时的旧值，
     * 且分数在及格线两侧变动时学分不会跟着调整。发布状态不由旧接口修改。</p>
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
        requireComponents(score);
        Integer studentId = score.getStudentId() != null ? score.getStudentId() : dbScore.getStudentId();
        Integer courseId = score.getCourseId() != null ? score.getCourseId() : dbScore.getCourseId();
        Course course = courseId == null ? null : courseMapper.selectById(courseId);
        if (course == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        boolean moved = !Objects.equals(studentId, dbScore.getStudentId()) || !Objects.equals(courseId, dbScore.getCourseId());
        if (moved) {
            // 改成了别的学生或别的课：不能和那边已有的成绩撞车
            Score clash = scoreMapper.selectByCourceIdAndStudentId(courseId, studentId);
            if (clash != null && !Objects.equals(clash.getId(), dbScore.getId())) {
                throw new CustomException(ResultCodeEnum.SCORE_ALREADY_ERROR);
            }
        }

        score.setStatus(null);
        score.setScore(totalOf(course, score));
        scoreMapper.updateById(score);

        // 学分按「是否计入」的迁移增减：不计入→计入 加，计入→不计入 减，状态不变则不动
        String status = GradebookService.statusOf(dbScore);
        boolean wasCounted = counts(status, dbScore.getScore());
        boolean nowCounted = counts(status, score.getScore());
        if (moved) {
            if (wasCounted) {
                adjustCredit(dbScore.getStudentId(), courseMapper.selectById(dbScore.getCourseId()), -1);
            }
            if (nowCounted) {
                adjustCredit(studentId, course, 1);
            }
        } else if (wasCounted != nowCounted) {
            adjustCredit(studentId, course, nowCounted ? 1 : -1);
        }
    }

    /**
     * 删除：仅当被删除的成绩原本计入了学分（已发布且及格）时才扣减学分。
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
        if (counts(GradebookService.statusOf(score), score.getScore())) {
            adjustCredit(score.getStudentId(), courseMapper.selectById(score.getCourseId()), -1);
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

    /** 学生看得到（已发布）的成绩行，草稿或不存在返回 null：旧接口改分后据此决定要不要通知学生 */
    public Score publishedRow(Integer id) {
        Score score = id == null ? null : scoreMapper.selectById(id);
        return score != null && GradebookService.isPublished(score) ? score : null;
    }

    /** 旧接口要求平时分、考试分都有，且在 0~100 之间 */
    private static void requireComponents(Score score) {
        if (score.getOrdinaryScore() == null || score.getExamScore() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (!GradeCalculator.validComponent(score.getOrdinaryScore())
                || !GradeCalculator.validComponent(score.getExamScore())) {
            throw new CustomException(ResultCodeEnum.SCORE_RANGE_ERROR);
        }
    }

    /**
     * 按课程权重算总评。课程给考勤、作业设了权重时，这两项按记录实时折算后一并写入；
     * 作业还没有可计入的成绩时，在已有的项里按权重归一（旧客户端录完平时、期末就能出总评）。
     */
    private Double totalOf(Course course, Score score) {
        int[] weights = GradeCalculator.weightsOf(course);
        if (weights[GradeCalculator.ATTENDANCE] > 0 || weights[GradeCalculator.HOMEWORK] > 0) {
            Integer studentId = score.getStudentId();
            if (studentId == null && score.getId() != null) {
                Score db = scoreMapper.selectById(score.getId());
                studentId = db == null ? null : db.getStudentId();
            }
            if (studentId != null) {
                Double[] points = gradebookService.livePoints(course.getId(), Collections.singletonList(studentId)).get(studentId);
                score.setAttendanceScore(points[0]);
                score.setHomeworkScore(points[1]);
            }
        }
        return GradeCalculator.totalWithFallback(weights, score.getAttendanceScore(), score.getHomeworkScore(),
                score.getOrdinaryScore(), score.getExamScore());
    }

    private static boolean counts(String status, Double total) {
        return GradeCalculator.counted(status == null ? GradeCalculator.PUBLISHED : status, total);
    }

    /**
     * 按课程学分调整学生学分。sign 为 +1 累加、-1 扣减。
     * 走 SQL 原子自增，避免并发录入成绩时的丢更新。
     */
    private void adjustCredit(Integer studentId, Course course, int sign) {
        if (studentId == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
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
     * 数据范围沿用 {@link #applyDataScope}，教师只统计本人任课、学生只统计本人；一律只统计已发布的成绩。
     */
    public Map<String, List<Object>> scoreDistribution() {
        Score probe = new Score();
        applyDataScope(probe);
        probe.setStatus(GradeCalculator.PUBLISHED);

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
     * 数据行级隔离：教师只能查看自己的成绩；学生只能查看自己已发布的成绩（分页与全量接口统一生效）
     */
    @Override
    protected void applyDataScope(Score score) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
            score.setTeacherId(currentUser.getId());
        }
        if (RoleEnum.STUDENT.name().equals(currentUser.getRole())) {
            score.setStudentId(currentUser.getId());
            score.setStatus(GradeCalculator.PUBLISHED);
        }
    }
}
