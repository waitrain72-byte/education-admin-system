package com.example.service;

import com.example.entity.Assignment;
import com.example.entity.Course;
import com.example.entity.Homework;

import java.util.List;
import java.util.Map;

/**
 * 成绩册的计算规则（纯函数，不碰数据库，便于单测和论文里说明）。
 *
 * <ul>
 *   <li>总评 = 考勤 × w1 + 作业 × w2 + 平时 × w3 + 期末 × w4（权重为百分比、合计 100），保留一位小数；
 *       权重大于 0 的项必须都有成绩，否则总评为空（待录）。</li>
 *   <li>考勤分：正常、请假记 100，迟到、早退记 80，缺勤记 0，取平均；没有考勤记录按全勤 100。</li>
 *   <li>作业分：已批改的按 得分 ÷ 满分 × 100；过了截止还没交的记 0；批改中、未到截止的不计入；
 *       没有可计入的作业时为空。</li>
 *   <li>学分只按「已发布且总评及格」的成绩累计。</li>
 * </ul>
 */
public final class GradeCalculator {

    public static final String DRAFT = "草稿";
    public static final String PUBLISHED = "已发布";
    public static final double PASS_LINE = 60;

    /** 权重下标：考勤、作业、平时、期末 */
    public static final int ATTENDANCE = 0;
    public static final int HOMEWORK = 1;
    public static final int ORDINARY = 2;
    public static final int EXAM = 3;

    private GradeCalculator() {
    }

    /** 课程的四项权重；没设置过的课程沿用改版前的「平时 30 + 期末 70」 */
    public static int[] weightsOf(Course course) {
        return new int[]{
                orDefault(course == null ? null : course.getWeightAttendance(), 0),
                orDefault(course == null ? null : course.getWeightHomework(), 0),
                orDefault(course == null ? null : course.getWeightOrdinary(), 30),
                orDefault(course == null ? null : course.getWeightExam(), 70),
        };
    }

    /** 每项 0~100、合计正好 100 */
    public static boolean validWeights(int[] weights) {
        if (weights == null || weights.length != 4) {
            return false;
        }
        int sum = 0;
        for (int w : weights) {
            if (w < 0 || w > 100) {
                return false;
            }
            sum += w;
        }
        return sum == 100;
    }

    /** 单项成绩是否合法：空（未录）或 0~100 */
    public static boolean validComponent(Double value) {
        return value == null || (!value.isNaN() && value >= 0 && value <= 100);
    }

    /**
     * 总评：有权重的项都录了才算，否则返回 null（待录）；权重为 0 的项不参与，录没录都行。
     */
    public static Double total(int[] weights, Double attendance, Double homework, Double ordinary, Double exam) {
        Double[] values = {attendance, homework, ordinary, exam};
        double sum = 0;
        for (int i = 0; i < 4; i++) {
            if (weights[i] <= 0) {
                continue;
            }
            if (values[i] == null) {
                return null;
            }
            sum += weights[i] * values[i];
        }
        return round1(sum / 100);
    }

    /**
     * 旧接口（小程序、旧版成绩页只录平时和期末）用的总评：先按完整规则算；
     * 缺项时在已有的项里按权重重新归一，保证旧客户端录完平时、期末就能出总评。
     * 一项都没有时返回 null。
     */
    public static Double totalWithFallback(int[] weights, Double attendance, Double homework, Double ordinary, Double exam) {
        Double full = total(weights, attendance, homework, ordinary, exam);
        if (full != null) {
            return full;
        }
        Double[] values = {attendance, homework, ordinary, exam};
        double sum = 0;
        int weightSum = 0;
        for (int i = 0; i < 4; i++) {
            if (weights[i] > 0 && values[i] != null) {
                sum += weights[i] * values[i];
                weightSum += weights[i];
            }
        }
        return weightSum == 0 ? null : round1(sum / weightSum);
    }

    /** 考勤分：statusCounts 为 考勤状态 → 次数；没有记录按全勤 */
    public static double attendancePoints(Map<String, Long> statusCounts) {
        long total = 0;
        double points = 0;
        if (statusCounts != null) {
            for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
                long count = entry.getValue() == null ? 0 : entry.getValue();
                total += count;
                points += count * pointOf(entry.getKey());
            }
        }
        return total == 0 ? 100 : round1(points / total);
    }

    private static double pointOf(String status) {
        if ("缺勤".equals(status)) {
            return 0;
        }
        if ("迟到".equals(status) || "早退".equals(status)) {
            return 80;
        }
        return 100;
    }

    /**
     * 作业分（百分制）。
     *
     * @param mine       该学生的提交，key 为作业任务 ID
     * @param nowMinutes 当前时间 yyyy-MM-dd HH:mm，用来判断是否已过截止
     */
    public static Double homeworkPoints(List<Assignment> assignments, Map<Integer, Homework> mine, String nowMinutes) {
        double sum = 0;
        int counted = 0;
        for (Assignment a : assignments) {
            Homework h = mine == null ? null : mine.get(a.getId());
            int full = a.getFullScore() == null || a.getFullScore() <= 0 ? 100 : a.getFullScore();
            if (h != null && "已批改".equals(h.getStatus()) && parse(h.getScore()) != null) {
                sum += Math.min(100, parse(h.getScore()) / full * 100);
                counted++;
            } else if (h == null && isPast(a.getDeadline(), nowMinutes)) {
                counted++;
            }
        }
        return counted == 0 ? null : round1(sum / counted);
    }

    /** 是否计入学分：已发布且及格 */
    public static boolean counted(String status, Double total) {
        return PUBLISHED.equals(status) && total != null && total >= PASS_LINE;
    }

    static boolean isPast(String deadline, String nowMinutes) {
        return deadline != null && !deadline.isEmpty() && nowMinutes != null && deadline.compareTo(nowMinutes) < 0;
    }

    static Double parse(String score) {
        if (score == null || score.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(score.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static int orDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
