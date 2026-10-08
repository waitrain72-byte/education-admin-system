package com.example.service;

import com.example.entity.Assignment;
import com.example.entity.Course;
import com.example.entity.Homework;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 成绩册计算规则：权重、总评、考勤分、作业分、学分计入条件。
 */
class GradeCalculatorTest {

    private static final String NOW = "2026-10-07 10:00";

    @Test
    @DisplayName("没设过权重的课程沿用改版前的「平时 30 + 期末 70」")
    void defaultWeights() {
        assertArrayEquals(new int[]{0, 0, 30, 70}, GradeCalculator.weightsOf(new Course()));
        assertArrayEquals(new int[]{0, 0, 30, 70}, GradeCalculator.weightsOf(null));
    }

    @Test
    @DisplayName("权重：每项 0~100、合计正好 100")
    void weightValidation() {
        assertTrue(GradeCalculator.validWeights(new int[]{10, 20, 20, 50}));
        assertTrue(GradeCalculator.validWeights(new int[]{0, 0, 0, 100}));
        assertFalse(GradeCalculator.validWeights(new int[]{10, 20, 20, 49}));
        assertFalse(GradeCalculator.validWeights(new int[]{-10, 20, 40, 50}));
        assertFalse(GradeCalculator.validWeights(new int[]{0, 0, 100}));
        assertFalse(GradeCalculator.validWeights(null));
    }

    @Test
    @DisplayName("单项成绩：空或 0~100")
    void componentValidation() {
        assertTrue(GradeCalculator.validComponent(null));
        assertTrue(GradeCalculator.validComponent(0.0));
        assertTrue(GradeCalculator.validComponent(100.0));
        assertFalse(GradeCalculator.validComponent(100.5));
        assertFalse(GradeCalculator.validComponent(-1.0));
        assertFalse(GradeCalculator.validComponent(Double.NaN));
    }

    @Test
    @DisplayName("总评：有权重的项都录了才算，保留一位小数")
    void totalNeedsEveryWeightedComponent() {
        int[] weights = {10, 20, 20, 50};
        // 100*0.1 + 80*0.2 + 60*0.2 + 90*0.5 = 83
        assertEquals(83.0, GradeCalculator.total(weights, 100.0, 80.0, 60.0, 90.0));
        assertNull(GradeCalculator.total(weights, 100.0, null, 60.0, 90.0));
        // 权重为 0 的项录没录都行
        assertEquals(81.0, GradeCalculator.total(new int[]{0, 0, 30, 70}, null, null, 60.0, 90.0));
        // 一位小数：33.3*0.3 + 66.7*0.7 = 9.99 + 46.69 = 56.68 → 56.7
        assertEquals(56.7, GradeCalculator.total(new int[]{0, 0, 30, 70}, null, null, 33.3, 66.7));
    }

    @Test
    @DisplayName("旧接口的总评：缺项时在已有的项里按权重归一；一项都没有时为空")
    void fallbackTotal() {
        int[] weights = {10, 20, 20, 50};
        // (100*10 + 60*20 + 90*50) / 80 = 83.75 → 83.8
        assertEquals(83.8, GradeCalculator.totalWithFallback(weights, 100.0, null, 60.0, 90.0));
        assertNull(GradeCalculator.totalWithFallback(weights, null, null, null, null));
        // 齐全时与 total 一致
        assertEquals(83.0, GradeCalculator.totalWithFallback(weights, 100.0, 80.0, 60.0, 90.0));
    }

    @Test
    @DisplayName("考勤分：正常、请假 100，迟到、早退 80，缺勤 0，取平均；没有记录按全勤")
    void attendancePoints() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("正常", 6L);
        counts.put("请假", 1L);
        counts.put("迟到", 1L);
        counts.put("早退", 1L);
        counts.put("缺勤", 1L);
        // (7*100 + 2*80 + 0) / 10 = 86
        assertEquals(86.0, GradeCalculator.attendancePoints(counts));
        assertEquals(100.0, GradeCalculator.attendancePoints(Collections.emptyMap()));
        assertEquals(100.0, GradeCalculator.attendancePoints(null));
    }

    @Test
    @DisplayName("作业分：已批改按得分率；过了截止没交记 0；批改中、未到截止的不计入")
    void homeworkPoints() {
        Assignment graded = assignment(1, 20, "2026-10-01 23:59");
        Assignment missed = assignment(2, 100, "2026-10-05 23:59");
        Assignment pending = assignment(3, 100, "2026-10-01 23:59");
        Assignment future = assignment(4, 100, "2026-10-20 23:59");

        Map<Integer, Homework> mine = new HashMap<>();
        mine.put(1, submission("已批改", "18"));
        mine.put(3, submission("已提交", null));

        List<Assignment> all = Arrays.asList(graded, missed, pending, future);
        // 计入两份：18/20 = 90，缺交 0 → 平均 45
        assertEquals(45.0, GradeCalculator.homeworkPoints(all, mine, NOW));
        // 只有未到截止、没批改的：没有可计入的，返回空
        assertNull(GradeCalculator.homeworkPoints(Arrays.asList(pending, future), mine, NOW));
        assertNull(GradeCalculator.homeworkPoints(Collections.emptyList(), null, NOW));
    }

    @Test
    @DisplayName("作业得分超过满分按 100 封顶，分数不是数字的不计入")
    void homeworkPointsEdgeCases() {
        Assignment a = assignment(1, 10, "2026-10-01 23:59");
        Map<Integer, Homework> mine = new HashMap<>();
        mine.put(1, submission("已批改", "12"));
        assertEquals(100.0, GradeCalculator.homeworkPoints(Collections.singletonList(a), mine, NOW));

        mine.put(1, submission("已批改", "优秀"));
        assertNull(GradeCalculator.homeworkPoints(Collections.singletonList(a), mine, NOW));
    }

    @Test
    @DisplayName("学分只按「已发布且及格」累计")
    void creditCountsOnlyPublishedPassing() {
        assertTrue(GradeCalculator.counted(GradeCalculator.PUBLISHED, 60.0));
        assertFalse(GradeCalculator.counted(GradeCalculator.PUBLISHED, 59.9));
        assertFalse(GradeCalculator.counted(GradeCalculator.DRAFT, 95.0));
        assertFalse(GradeCalculator.counted(GradeCalculator.PUBLISHED, null));
    }

    @Test
    @DisplayName("截止判断：截止时间早于现在才算过了截止")
    void deadlineComparison() {
        assertTrue(GradeCalculator.isPast("2026-10-07 09:59", NOW));
        assertFalse(GradeCalculator.isPast("2026-10-07 10:00", NOW));
        assertFalse(GradeCalculator.isPast(null, NOW));
        assertFalse(GradeCalculator.isPast("", NOW));
    }

    private static Assignment assignment(int id, int fullScore, String deadline) {
        Assignment a = new Assignment();
        a.setId(id);
        a.setFullScore(fullScore);
        a.setDeadline(deadline);
        return a;
    }

    private static Homework submission(String status, String score) {
        Homework h = new Homework();
        h.setStatus(status);
        h.setScore(score);
        return h;
    }
}
