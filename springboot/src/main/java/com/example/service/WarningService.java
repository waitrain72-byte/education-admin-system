package com.example.service;

import com.example.entity.Account;
import com.example.entity.Attendance;
import com.example.entity.Score;
import com.example.entity.Student;
import com.example.mapper.AttendanceMapper;
import com.example.mapper.ScoreMapper;
import com.example.mapper.StudentMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 学业预警服务：多指标加权评分模型。
 *
 * <p>风险指数 riskIndex ∈ [0,100]，越高风险越大，由三项指标加权合成：</p>
 * <ol>
 *   <li>不及格占比（权重 40）：不及格门数 / 有成绩门数，占比达 30% 记满分；</li>
 *   <li>平均分差距（权重 30）：平均分低于 80 分开始扣分，每低 1 分扣 0.75 分（低 40 分记满分）；</li>
 *   <li>异常考勤率（权重 30）：(缺勤×1 + 迟到×0.5 + 早退×0.5) / 考勤总次数。</li>
 * </ol>
 * <p>等级划分：≥60 高风险 / ≥30 中风险 / ≥10 低风险 / &lt;10 正常。</p>
 *
 * <p>数据范围：管理员看全部学生，教师仅看本人任课课程关联的学生，学生仅看本人。
 * 指标权重与阈值为可调参数，集中定义于常量，便于论文实验部分做灵敏度分析。</p>
 */
@Service
public class WarningService {

    /** 指标权重（合计 100） */
    private static final double WEIGHT_FAILED = 40;
    private static final double WEIGHT_AVG = 30;
    private static final double WEIGHT_ATTENDANCE = 30;
    /** 不及格占比达到该值记满分 */
    private static final double FAILED_FULL_RATIO = 0.3;
    /** 平均分基准线：低于该值开始扣分 */
    private static final double AVG_BASELINE = 80;
    /** 平均分扣满权重对应的差距 */
    private static final double AVG_FULL_GAP = 40;
    /** 迟到/早退的异常折算系数（缺勤为 1） */
    private static final double HALF_ABNORMAL = 0.5;
    /** 等级阈值 */
    private static final double LEVEL_HIGH = 60;
    private static final double LEVEL_MIDDLE = 30;
    private static final double LEVEL_LOW = 10;

    @Resource
    private ScoreMapper scoreMapper;
    @Resource
    private AttendanceMapper attendanceMapper;
    @Resource
    private StudentMapper studentMapper;

    /**
     * 计算学业预警列表（按风险指数降序）。
     * 数据范围由当前登录角色决定：ADMIN 全部 / TEACHER 本人任课学生 / STUDENT 仅本人。
     */
    public List<Map<String, Object>> listWarnings() {
        Account current = TokenUtils.getCurrentUser();
        String role = current.getRole() == null ? "" : current.getRole();
        Integer selfId = current.getId();

        // 1. 加载原始数据并按学生聚合（无成绩的学生若仅有考勤记录同样参与计算）
        Map<Integer, List<Score>> scoresByStudent = new HashMap<>();
        for (Score sc : scoreMapper.selectAll(new Score())) {
            if (sc.getStudentId() == null || sc.getScore() == null) {
                continue;
            }
            scoresByStudent.computeIfAbsent(sc.getStudentId(), k -> new ArrayList<>()).add(sc);
        }
        Map<Integer, List<Attendance>> attendanceByStudent = new HashMap<>();
        for (Attendance at : attendanceMapper.selectAll(new Attendance())) {
            if (at.getStudentId() == null) {
                continue;
            }
            attendanceByStudent.computeIfAbsent(at.getStudentId(), k -> new ArrayList<>()).add(at);
        }

        Map<Integer, String> studentNames = new HashMap<>();
        for (Student s : studentMapper.selectAll(new Student())) {
            studentNames.put(s.getId(), s.getName());
        }

        Set<Integer> studentIds = new HashSet<>(scoresByStudent.keySet());
        studentIds.addAll(attendanceByStudent.keySet());

        // 2. 逐学生计算风险指数
        List<Map<String, Object>> result = new ArrayList<>();
        for (Integer sid : studentIds) {
            // 数据范围过滤
            if ("STUDENT".equals(role) && !sid.equals(selfId)) {
                continue;
            }
            if ("TEACHER".equals(role) && !relatedToTeacher(sid, selfId, scoresByStudent, attendanceByStudent)) {
                continue;
            }

            List<Score> myScores = scoresByStudent.getOrDefault(sid, Collections.emptyList());
            List<Attendance> myAttendance = attendanceByStudent.getOrDefault(sid, Collections.emptyList());

            double avg = myScores.stream().mapToDouble(Score::getScore).average().orElse(AVG_BASELINE);
            long failed = myScores.stream().filter(s -> s.getScore() < 60).count();

            int abnormalUnits = 0;
            for (Attendance at : myAttendance) {
                if ("缺勤".equals(at.getStatus())) {
                    abnormalUnits += 2;
                } else if ("迟到".equals(at.getStatus()) || "早退".equals(at.getStatus())) {
                    abnormalUnits += 1;
                }
            }
            // abnormalUnits 以 2 为满分单位：缺勤 1 次即占满 1 次考勤的异常额度
            double absentRate = myAttendance.isEmpty() ? 0 : (double) abnormalUnits / (myAttendance.size() * 2);

            // 3. 加权合成风险指数
            double failPart = myScores.isEmpty() ? 0 : Math.min(failed / (double) myScores.size() / FAILED_FULL_RATIO, 1) * WEIGHT_FAILED;
            double avgPart = Math.max(0, Math.min((AVG_BASELINE - avg) / AVG_FULL_GAP, 1)) * WEIGHT_AVG;
            double attendancePart = absentRate * WEIGHT_ATTENDANCE;
            int riskIndex = (int) Math.round(failPart + avgPart + attendancePart);

            Map<String, Object> row = new HashMap<>();
            row.put("studentId", sid);
            row.put("studentName", studentNames.getOrDefault(sid, "未知学生"));
            row.put("courseCount", myScores.size());
            row.put("avgScore", Math.round(avg * 10) / 10.0);
            row.put("failedCount", failed);
            row.put("absentRate", Math.round(absentRate * 100));
            row.put("riskIndex", riskIndex);
            row.put("level", levelOf(riskIndex));
            row.put("suggestion", suggestionOf(riskIndex));
            result.add(row);
        }

        result.sort((a, b) -> Integer.compare((int) b.get("riskIndex"), (int) a.get("riskIndex")));
        return result;
    }

    /** 教师数据范围：该学生的成绩或考勤记录中存在本人任课（teacher_id = 当前教师） */
    private boolean relatedToTeacher(Integer studentId, Integer teacherId,
                                     Map<Integer, List<Score>> scoresByStudent,
                                     Map<Integer, List<Attendance>> attendanceByStudent) {
        for (Score sc : scoresByStudent.getOrDefault(studentId, Collections.emptyList())) {
            if (teacherId.equals(sc.getTeacherId())) {
                return true;
            }
        }
        for (Attendance at : attendanceByStudent.getOrDefault(studentId, Collections.emptyList())) {
            if (teacherId.equals(at.getTeacherId())) {
                return true;
            }
        }
        return false;
    }

    private String levelOf(int riskIndex) {
        if (riskIndex >= LEVEL_HIGH) {
            return "高风险";
        }
        if (riskIndex >= LEVEL_MIDDLE) {
            return "中风险";
        }
        if (riskIndex >= LEVEL_LOW) {
            return "低风险";
        }
        return "正常";
    }

    private String suggestionOf(int riskIndex) {
        if (riskIndex >= LEVEL_HIGH) {
            return "多门课程不及格或出勤异常，建议尽快约谈并制定帮扶计划";
        }
        if (riskIndex >= LEVEL_MIDDLE) {
            return "成绩或出勤存在明显波动，建议重点关注课堂表现与作业完成情况";
        }
        if (riskIndex >= LEVEL_LOW) {
            return "个别指标偏低，建议保持关注";
        }
        return "学业状态良好";
    }
}
