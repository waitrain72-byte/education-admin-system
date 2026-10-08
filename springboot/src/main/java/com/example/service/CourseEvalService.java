package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.AppTime;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Course;
import com.example.entity.CourseEval;
import com.example.exception.CustomException;
import com.example.mapper.CourseEvalMapper;
import com.example.mapper.CourseSpaceMapper;
import com.example.utils.TokenUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程评价：课程结课后，选课学生从五个维度（教学态度、教学内容、教学方法、教学效果、课后辅导）各打 1~5 星，
 * 可附一段文字；每人每门课只能评一次。老师看到的是汇总和匿名的文字评价。
 */
@Service
public class CourseEvalService {

    static final String FINISHED = "已结课";
    static final int MAX_COMMENT = 500;
    /** 五个维度在返回数据里的键名（与实体字段一致） */
    static final String[] DIMENSIONS = {"attitude", "contentScore", "method", "effect", "support"};

    private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private CourseSpaceService courseSpaceService;
    @Resource
    private CourseEvalMapper courseEvalMapper;
    @Resource
    private CourseSpaceMapper courseSpaceMapper;

    /**
     * 评价页数据：学生 = 是否开放、自己的评价；老师和管理员 = 汇总（各维度均分、综合分、星级分布）+ 匿名文字评价。
     */
    public Map<String, Object> view(Integer courseId) {
        Course course = courseSpaceService.requireCourse(courseId);
        String relation = courseSpaceService.requireMember(course);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("open", FINISHED.equals(course.getStatus()));

        if (CourseSpaceService.REL_STUDENT.equals(relation)) {
            data.put("mine", courseEvalMapper.selectByCourseAndStudent(courseId, TokenUtils.getCurrentUser().getId()));
            return data;
        }

        List<CourseEval> evals = courseEvalMapper.selectByCourse(courseId);
        data.put("count", evals.size());
        data.put("students", courseSpaceMapper.selectMemberIds(courseId).size());
        data.putAll(summarize(evals));
        List<Map<String, Object>> comments = new ArrayList<>();
        for (CourseEval e : evals) {
            if (StrUtil.isNotBlank(e.getComment())) {
                // 匿名：不带学生信息
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", e.getId());
                item.put("overall", overall(e));
                item.put("comment", e.getComment());
                item.put("createTime", e.getCreateTime());
                comments.add(item);
            }
        }
        data.put("comments", comments);
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public CourseEval submit(Integer courseId, CourseEval form) {
        Course course = courseSpaceService.requireCourse(courseId);
        if (!CourseSpaceService.REL_STUDENT.equals(courseSpaceService.relationOf(course))) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        if (!FINISHED.equals(course.getStatus())) {
            throw new CustomException(ResultCodeEnum.EVAL_NOT_OPEN_ERROR);
        }
        if (form == null || !validStar(form.getAttitude()) || !validStar(form.getContentScore())
                || !validStar(form.getMethod()) || !validStar(form.getEffect()) || !validStar(form.getSupport())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        String comment = StrUtil.trimToNull(form.getComment());
        if (comment != null && comment.length() > MAX_COMMENT) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Integer studentId = TokenUtils.getCurrentUser().getId();
        if (courseEvalMapper.selectByCourseAndStudent(courseId, studentId) != null) {
            throw new CustomException(ResultCodeEnum.EVAL_ALREADY_ERROR);
        }

        CourseEval eval = new CourseEval();
        eval.setCourseId(courseId);
        eval.setTeacherId(course.getTeacherId());
        eval.setStudentId(studentId);
        eval.setAttitude(form.getAttitude());
        eval.setContentScore(form.getContentScore());
        eval.setMethod(form.getMethod());
        eval.setEffect(form.getEffect());
        eval.setSupport(form.getSupport());
        eval.setComment(comment);
        eval.setCreateTime(LocalDateTime.now(AppTime.clock()).format(MINUTES));
        try {
            courseEvalMapper.insert(eval);
        } catch (DuplicateKeyException e) {
            // 同一学生并发提交两次：唯一索引兜底
            throw new CustomException(ResultCodeEnum.EVAL_ALREADY_ERROR);
        }
        return eval;
    }

    /** 各维度均分、综合均分（五项平均）、综合星级分布（四舍五入到 1~5 星） */
    static Map<String, Object> summarize(List<CourseEval> evals) {
        double[] sums = new double[DIMENSIONS.length];
        int[] stars = new int[5];
        double overallSum = 0;
        for (CourseEval e : evals) {
            int[] values = valuesOf(e);
            for (int i = 0; i < values.length; i++) {
                sums[i] += values[i];
            }
            double overall = overall(e);
            overallSum += overall;
            int star = (int) Math.max(1, Math.min(5, Math.round(overall)));
            stars[star - 1]++;
        }
        Map<String, Object> averages = new LinkedHashMap<>();
        for (int i = 0; i < DIMENSIONS.length; i++) {
            averages.put(DIMENSIONS[i], evals.isEmpty() ? null : round2(sums[i] / evals.size()));
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("averages", averages);
        summary.put("overall", evals.isEmpty() ? null : round2(overallSum / evals.size()));
        summary.put("stars", stars);
        return summary;
    }

    static double overall(CourseEval e) {
        int[] values = valuesOf(e);
        double sum = 0;
        for (int v : values) {
            sum += v;
        }
        return round2(sum / values.length);
    }

    private static int[] valuesOf(CourseEval e) {
        return new int[]{orZero(e.getAttitude()), orZero(e.getContentScore()), orZero(e.getMethod()),
                orZero(e.getEffect()), orZero(e.getSupport())};
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static boolean validStar(Integer value) {
        return value != null && value >= 1 && value <= 5;
    }

    private static double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
