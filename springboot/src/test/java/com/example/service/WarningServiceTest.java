package com.example.service;

import com.example.entity.Attendance;
import com.example.entity.Score;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 学业预警评分模型单元测试。
 *
 * <p>{@link WarningService#computeRisk} 是纯函数（不碰数据库），可直接 new 出 Service 测试。
 * 覆盖三项指标各自的边界与加权合成结果，也是论文中权重灵敏度分析的验证依据。</p>
 *
 * <p>模型（权重合计 100）：不及格占比 40 / 平均分差距 30 / 异常考勤率 30；
 * 平均分基准线 80、扣满差距 40；不及格占比达 30% 记满分。</p>
 */
class WarningServiceTest {

    private final WarningService service = new WarningService();

    private Score score(double value) {
        Score s = new Score();
        s.setStudentId(1);
        s.setScore(value);
        return s;
    }

    private Attendance attendance(String status) {
        Attendance a = new Attendance();
        a.setStudentId(1);
        a.setStatus(status);
        return a;
    }

    private List<Score> scores(double... values) {
        List<Score> list = new ArrayList<>();
        for (double v : values) {
            list.add(score(v));
        }
        return list;
    }

    private List<Attendance> attendances(String... statuses) {
        List<Attendance> list = new ArrayList<>();
        for (String s : statuses) {
            list.add(attendance(s));
        }
        return list;
    }

    @Test
    @DisplayName("无任何数据：风险指数为 0，等级正常")
    void emptyDataGivesZeroRisk() {
        WarningService.RiskResult risk = service.computeRisk(Collections.emptyList(), Collections.emptyList());
        assertEquals(0, risk.riskIndex);
        assertEquals(0, risk.courseCount);
        assertEquals(0, risk.failedCount);
        assertEquals(0, risk.absentRate);
        // 无成绩时平均分取基准线 80，不产生平均分扣分
        assertEquals(80.0, risk.avgScore);
    }

    @Test
    @DisplayName("平均分正好等于基准线 80 且全及格：不扣分")
    void averageAtBaselineGivesZeroRisk() {
        WarningService.RiskResult risk = service.computeRisk(scores(80, 80), Collections.emptyList());
        assertEquals(0, risk.riskIndex);
        assertEquals(2, risk.courseCount);
        assertEquals(0, risk.failedCount);
    }

    @Test
    @DisplayName("只有考勤全勤：风险指数为 0")
    void fullAttendanceGivesZeroRisk() {
        WarningService.RiskResult risk = service.computeRisk(Collections.emptyList(), attendances("正常", "正常", "正常"));
        assertEquals(0, risk.riskIndex);
        assertEquals(0, risk.absentRate);
    }

    @Test
    @DisplayName("考勤全缺勤：异常考勤率 100%，该项拿满 30 分")
    void allAbsentGivesFullAttendanceWeight() {
        WarningService.RiskResult risk = service.computeRisk(Collections.emptyList(), attendances("缺勤", "缺勤"));
        assertEquals(100, risk.absentRate);
        assertEquals(30, risk.riskIndex);
    }

    @Test
    @DisplayName("迟到/早退按半次折算：2 次迟到 = 1 次缺勤的异常额度")
    void lateCountsAsHalfAbsence() {
        WarningService.RiskResult late = service.computeRisk(Collections.emptyList(), attendances("迟到", "迟到"));
        WarningService.RiskResult absent = service.computeRisk(Collections.emptyList(), attendances("缺勤", "正常"));
        assertEquals(50, late.absentRate);
        assertEquals(absent.absentRate, late.absentRate);
    }

    @Test
    @DisplayName("不及格占比达到 30% 阈值即拿满 40 分（超过也不再增加）")
    void failedRatioCapsAtFullWeight() {
        // 10 门中 3 门不及格 = 30%，正好达到满分阈值
        WarningService.RiskResult atThreshold =
                service.computeRisk(scores(50, 50, 50, 80, 80, 80, 80, 80, 80, 80), Collections.emptyList());
        // 10 门中 6 门不及格 = 60%，远超阈值
        WarningService.RiskResult beyond =
                service.computeRisk(scores(50, 50, 50, 50, 50, 50, 80, 80, 80, 80), Collections.emptyList());

        assertEquals(3, atThreshold.failedCount);
        // 达到阈值时不及格项已满，两者的差异只可能来自平均分项
        assertTrue(beyond.riskIndex > atThreshold.riskIndex,
                "不及格更多时平均分更低，总分应更高");
    }

    @Test
    @DisplayName("平均分低于基准线 40 分以上：平均分项拿满 30 分")
    void averageGapCapsAtFullWeight() {
        // 平均 40 分，与基准线 80 差 40 分，平均分项满分；且全部不及格，不及格项也满分
        WarningService.RiskResult risk = service.computeRisk(scores(40, 40), Collections.emptyList());
        assertEquals(40.0, risk.avgScore);
        assertEquals(2, risk.failedCount);
        // 不及格 40 + 平均分 30 = 70（无考勤记录，考勤项为 0）
        assertEquals(70, risk.riskIndex);
    }

    @Test
    @DisplayName("三项指标全部拉满：风险指数为 100")
    void allIndicatorsMaxedGivesHundred() {
        WarningService.RiskResult risk = service.computeRisk(scores(20, 30), attendances("缺勤", "缺勤"));
        assertEquals(100, risk.riskIndex);
    }

    @Test
    @DisplayName("平均分保留一位小数")
    void averageIsRoundedToOneDecimal() {
        WarningService.RiskResult risk = service.computeRisk(scores(80, 85, 81), Collections.emptyList());
        assertEquals(82.0, risk.avgScore);

        WarningService.RiskResult risk2 = service.computeRisk(scores(80, 81), Collections.emptyList());
        assertEquals(80.5, risk2.avgScore);
    }

    @Test
    @DisplayName("等级阈值：>=60 高风险 / >=30 中风险 / >=10 低风险 / <10 正常")
    void levelBoundaries() {
        assertEquals("高风险", service.levelOf(60));
        assertEquals("中风险", service.levelOf(59));
        assertEquals("中风险", service.levelOf(30));
        assertEquals("低风险", service.levelOf(29));
        assertEquals("低风险", service.levelOf(10));
        assertEquals("正常", service.levelOf(9));
        assertEquals("正常", service.levelOf(0));
    }

    @Test
    @DisplayName("端到端：由考勤构造的风险指数能落到预期等级")
    void riskIndexMapsToLevel() {
        // 只用考勤项构造精确的风险指数：20 次考勤中 n 次缺勤 -> 风险指数 = round(n/20 * 30)
        assertEquals("正常", service.levelOf(riskOfAbsences(0)));      // 0
        assertEquals("低风险", service.levelOf(riskOfAbsences(7)));     // round(10.5) = 11（含 .5 进位）
        assertEquals("中风险", service.levelOf(riskOfAbsences(20)));    // 30
        // 高风险单靠考勤项（上限 30）达不到，需叠加成绩项
        assertEquals("高风险", service.levelOf(
                service.computeRisk(scores(20, 30), attendances("缺勤", "缺勤")).riskIndex));
    }

    /** 构造「20 次考勤中 absences 次缺勤」的场景，返回风险指数 */
    private int riskOfAbsences(int absences) {
        String[] statuses = new String[20];
        Arrays.fill(statuses, "正常");
        for (int i = 0; i < absences; i++) {
            statuses[i] = "缺勤";
        }
        return service.computeRisk(Collections.emptyList(), attendances(statuses)).riskIndex;
    }
}
