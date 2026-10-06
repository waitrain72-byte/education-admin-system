package com.example.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 首页聚合里的纯计算：节次时间解析、今日课程筛选排序、绩点、出勤率
 */
class WorkbenchServiceTest {

    private static final String SEG1 = "第一大节（08:30 ~ 10:10）";
    private static final String SEG2 = "第二大节（10:30 ~ 12:10）";
    private static final String SEG3 = "第三大节（14:00 ~ 15:40）";

    @Test
    @DisplayName("节次时间：从中文节次里解析起止时间，解析不到返回空串")
    void segmentTimes() {
        assertArrayEquals(new String[]{"08:30", "10:10"}, WorkbenchService.segmentTimes(SEG1));
        assertArrayEquals(new String[]{"09:05", "10:00"}, WorkbenchService.segmentTimes("加课（9:05~10:00）"));
        assertArrayEquals(new String[]{"", ""}, WorkbenchService.segmentTimes("第一大节"));
        assertArrayEquals(new String[]{"", ""}, WorkbenchService.segmentTimes(null));
    }

    @Test
    @DisplayName("今日课程：只要正在开课、星期对得上的，按节次先后排")
    void todayCourses() {
        List<Map<String, Object>> courses = new ArrayList<>();
        courses.add(card(1, "星期三", SEG3, "已开课"));
        courses.add(card(2, "星期三", SEG1, "已开课"));
        courses.add(card(3, "星期三", SEG2, "已结课"));
        courses.add(card(4, "星期四", SEG1, "已开课"));
        courses.add(card(5, "星期三", SEG2, "未开课"));

        List<Map<String, Object>> today = WorkbenchService.todayCourses(courses, "星期三");
        assertEquals(Arrays.asList(2, 1), Arrays.asList(today.get(0).get("id"), today.get(1).get("id")));
        assertEquals(2, today.size());
        assertEquals("08:30", today.get(0).get("start"));
    }

    @Test
    @DisplayName("绩点：及格（成绩−50）÷10、上限 5，不及格 0，按学分加权，保留两位")
    void gpa() {
        assertEquals(1.0, WorkbenchService.gradePoint(60));
        assertEquals(4.0, WorkbenchService.gradePoint(90));
        assertEquals(5.0, WorkbenchService.gradePoint(100));
        assertEquals(0.0, WorkbenchService.gradePoint(59.9));

        // (4.0*3 + 0*2) / 5 = 2.4
        assertEquals(2.4, WorkbenchService.gpa(Arrays.asList(score(90, 3), score(40, 2))));
        // 学分缺失或为 0 的课不参与加权
        assertEquals(3.5, WorkbenchService.gpa(Arrays.asList(score(85, 2), score(100, 0), score(70, null))));
        assertNull(WorkbenchService.gpa(Collections.emptyList()));
    }

    @Test
    @DisplayName("出勤率：迟到早退算出勤，缺勤不算，请假不进分母；没有记录为 null")
    void attendanceRate() {
        List<Map<String, Object>> rows = Arrays.asList(status("正常", 16), status("迟到", 2), status("缺勤", 2), status("请假", 5));
        assertEquals(90.0, WorkbenchService.attendanceRate(rows));
        assertNull(WorkbenchService.attendanceRate(Collections.singletonList(status("请假", 3))));
        assertNull(WorkbenchService.attendanceRate(Collections.emptyList()));
    }

    private Map<String, Object> card(int id, String week, String segment, String status) {
        return WorkbenchService.courseCard(id, "课程" + id, "必修", 3, "路易斯", "7701", week, segment, status, 50);
    }

    private Map<String, Object> score(double score, Integer credit) {
        Map<String, Object> row = new HashMap<>();
        row.put("score", score);
        row.put("credit", credit);
        return row;
    }

    private Map<String, Object> status(String name, long value) {
        Map<String, Object> row = new HashMap<>();
        row.put("name", name);
        row.put("value", value);
        return row;
    }
}
