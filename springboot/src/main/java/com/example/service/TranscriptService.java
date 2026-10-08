package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Score;
import com.example.entity.Student;
import com.example.exception.CustomException;
import com.example.mapper.ScoreMapper;
import com.example.mapper.StudentMapper;
import com.example.mapper.WorkbenchMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生成绩单：所有课一张表（课程空间里只能一门一门看），只列已发布的成绩，没发布的显示「未出分」；
 * 汇总已修学分、平均学分绩点、加权平均分。学分与绩点的算法和首页一致（{@link WorkbenchService}）。
 */
@Service
public class TranscriptService {

    @Resource
    private ScoreMapper scoreMapper;
    @Resource
    private StudentMapper studentMapper;
    @Resource
    private WorkbenchMapper workbenchMapper;

    public Map<String, Object> mine() {
        Account current = TokenUtils.getCurrentUser();
        if (!RoleEnum.STUDENT.name().equals(current.getRole())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        Integer studentId = current.getId();

        Score probe = new Score();
        probe.setStudentId(studentId);
        probe.setStatus(GradeCalculator.PUBLISHED);
        // 历史数据里同一门课可能有两行成绩，取最新的一行
        Map<Integer, Score> published = new HashMap<>();
        for (Score s : scoreMapper.selectAll(probe)) {
            if (s.getCourseId() != null && s.getScore() != null) {
                published.merge(s.getCourseId(), s, (a, b) -> a.getId() >= b.getId() ? a : b);
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        int passed = 0;
        int failed = 0;
        int pending = 0;
        double creditSum = 0;
        double weightedSum = 0;
        for (Map<String, Object> course : scoreMapper.selectTranscriptCourses(studentId)) {
            Map<String, Object> row = new LinkedHashMap<>(course);
            Score score = published.get(toInt(course.get("courseId")));
            row.put("published", score != null);
            if (score == null) {
                pending++;
                rows.add(row);
                continue;
            }
            double total = score.getScore();
            boolean pass = total >= GradeCalculator.PASS_LINE;
            row.put("attendanceScore", score.getAttendanceScore());
            row.put("homeworkScore", score.getHomeworkScore());
            row.put("ordinaryScore", score.getOrdinaryScore());
            row.put("examScore", score.getExamScore());
            row.put("total", total);
            row.put("passed", pass);
            row.put("gradePoint", Math.round(WorkbenchService.gradePoint(total) * 10) / 10.0);
            if (pass) {
                passed++;
            } else {
                failed++;
            }
            Number credit = (Number) course.get("credit");
            if (credit != null && credit.doubleValue() > 0) {
                creditSum += credit.doubleValue();
                weightedSum += total * credit.doubleValue();
            }
            rows.add(row);
        }
        // 出了分的在前（新课在上），没出分的放后面
        rows.sort(Comparator.<Map<String, Object>>comparingInt(r -> Boolean.TRUE.equals(r.get("published")) ? 0 : 1)
                .thenComparing(r -> -toInt(r.get("courseId"))));

        Student student = studentMapper.selectById(studentId);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("credits", student == null || student.getScore() == null ? 0 : student.getScore());
        summary.put("requiredCredits", workbenchMapper.requiredCredits(studentId));
        summary.put("gpa", WorkbenchService.gpa(workbenchMapper.scoresWithCredit(studentId)));
        summary.put("average", creditSum == 0 ? null : Math.round(weightedSum / creditSum * 10) / 10.0);
        summary.put("passed", passed);
        summary.put("failed", failed);
        summary.put("pending", pending);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", summary);
        data.put("rows", rows);
        return data;
    }

    private static int toInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }
}
