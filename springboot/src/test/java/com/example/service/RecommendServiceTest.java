package com.example.service;

import com.example.entity.Course;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 课程推荐（基于物品的协同过滤）单元测试。
 *
 * <p>{@link RecommendService#rank} 是纯函数（打分输入由调用方给定，不碰数据库），
 * 可直接构造「学生 × 课程」选课矩阵来验证余弦相似度、冷启动降级与排序规则。</p>
 *
 * <p>相似度定义：sim(A,B) = |U(A) ∩ U(B)| / sqrt(|U(A)| × |U(B)|)，
 * 推荐得分 score(C) = Σ sim(C, s)，s 取遍目标学生已选课程。</p>
 */
class RecommendServiceTest {

    private final RecommendService service = new RecommendService();

    private Course course(int id, String name) {
        Course c = new Course();
        c.setId(id);
        c.setName(name);
        c.setType("选修");
        c.setScore(2);
        c.setTeacherName("张老师");
        return c;
    }

    private Map<Integer, Course> courses(int... ids) {
        Map<Integer, Course> map = new LinkedHashMap<>();
        for (int id : ids) {
            map.put(id, course(id, "课程" + id));
        }
        return map;
    }

    private Set<Integer> students(Integer... ids) {
        return new HashSet<>(Arrays.asList(ids));
    }

    private List<Integer> idsOf(List<Map<String, Object>> rows) {
        return rows.stream().map(r -> (Integer) r.get("id")).collect(Collectors.toList());
    }

    @Test
    @DisplayName("已选课程不出现在推荐结果中")
    void selectedCoursesAreExcluded() {
        Map<Integer, Set<Integer>> courseUsers = new HashMap<>();
        courseUsers.put(1, students(100, 200));
        courseUsers.put(2, students(100));
        courseUsers.put(3, students(200));

        List<Map<String, Object>> result = service.rank(courseUsers, courses(1, 2, 3), students(1), 10);

        assertFalse(idsOf(result).contains(1), "课程 1 已选，不应被推荐");
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("余弦相似度：完全重合的两门课相似度为 1，推荐理由显示 100%")
    void identicalUserSetsGiveSimilarityOne() {
        Map<Integer, Set<Integer>> courseUsers = new HashMap<>();
        // 课程 1 与课程 2 的选课学生完全相同 -> sim = 2 / sqrt(2*2) = 1
        courseUsers.put(1, students(100, 200));
        courseUsers.put(2, students(100, 200));

        List<Map<String, Object>> result = service.rank(courseUsers, courses(1, 2), students(1), 10);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).get("id"));
        assertEquals("与已选《课程1》相似度 100%", result.get(0).get("reason"));
    }

    @Test
    @DisplayName("余弦相似度：无共同学生的课程相似度为 0，降级为热门理由")
    void disjointUserSetsGiveZeroSimilarity() {
        Map<Integer, Set<Integer>> courseUsers = new HashMap<>();
        courseUsers.put(1, students(100));
        courseUsers.put(2, students(200, 300));

        List<Map<String, Object>> result = service.rank(courseUsers, courses(1, 2), students(1), 10);

        assertEquals(1, result.size());
        // 无相似命中 -> 用热门理由（该课选课人数）
        assertEquals("2 人已选", result.get(0).get("reason"));
        assertEquals(2, result.get(0).get("popularity"));
    }

    @Test
    @DisplayName("相似度高的课程排在前面")
    void higherSimilarityRanksFirst() {
        Map<Integer, Set<Integer>> courseUsers = new HashMap<>();
        courseUsers.put(1, students(100, 200, 300));  // 目标学生已选
        courseUsers.put(2, students(100, 200, 300));  // 完全重合，sim = 1
        courseUsers.put(3, students(100, 400, 500));  // 交集 1 个，sim = 1/3

        List<Map<String, Object>> result = service.rank(courseUsers, courses(1, 2, 3), students(1), 10);

        assertEquals(Arrays.asList(2, 3), idsOf(result));
    }

    @Test
    @DisplayName("冷启动：学生无任何选课记录时，按选课人数（热门）排序")
    void coldStartFallsBackToPopularity() {
        Map<Integer, Set<Integer>> courseUsers = new HashMap<>();
        courseUsers.put(1, students(100));
        courseUsers.put(2, students(100, 200, 300));
        courseUsers.put(3, students(100, 200));

        List<Map<String, Object>> result = service.rank(courseUsers, courses(1, 2, 3), Collections.emptySet(), 10);

        // 得分全为 0，按 popularity 降序：课程2(3人) > 课程3(2人) > 课程1(1人)
        assertEquals(Arrays.asList(2, 3, 1), idsOf(result));
        assertEquals("3 人已选", result.get(0).get("reason"));
    }

    @Test
    @DisplayName("得分相同时按选课人数决定先后")
    void tieBreaksByPopularity() {
        Map<Integer, Set<Integer>> courseUsers = new HashMap<>();
        // 目标学生已选课程 1，课程 2 与课程 3 都与其无交集（得分同为 0）
        courseUsers.put(1, students(100));
        courseUsers.put(2, students(200));
        courseUsers.put(3, students(200, 300, 400));

        List<Map<String, Object>> result = service.rank(courseUsers, courses(1, 2, 3), students(1), 10);

        assertEquals(Arrays.asList(3, 2), idsOf(result));
    }

    @Test
    @DisplayName("limit 截断推荐条数")
    void limitTruncatesResult() {
        Map<Integer, Set<Integer>> courseUsers = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            courseUsers.put(i, students(100));
        }

        List<Map<String, Object>> result = service.rank(courseUsers, courses(1, 2, 3, 4, 5), Collections.emptySet(), 2);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("无人选过的课程不会因除零而报错，得分记 0")
    void zeroPopularityCourseIsSafe() {
        Map<Integer, Set<Integer>> courseUsers = new HashMap<>();
        courseUsers.put(1, students(100));
        // 课程 2 无人选过：分母 sqrt(0 * 1) = 0，需按 0 处理而不是 NaN

        List<Map<String, Object>> result = service.rank(courseUsers, courses(1, 2), students(1), 10);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).get("popularity"));
        assertEquals("0 人已选", result.get(0).get("reason"));
    }

    @Test
    @DisplayName("推荐卡片字段齐全，且教师名取自课程自带的 teacherName")
    void resultCarriesDisplayFields() {
        Map<Integer, Set<Integer>> courseUsers = new HashMap<>();
        courseUsers.put(1, students(100));

        List<Map<String, Object>> result = service.rank(courseUsers, courses(1, 2), Collections.emptySet(), 10);

        Map<String, Object> row = result.get(0);
        assertTrue(row.containsKey("id"));
        assertTrue(row.containsKey("name"));
        assertTrue(row.containsKey("type"));
        assertTrue(row.containsKey("credit"));
        assertTrue(row.containsKey("reason"));
        assertEquals("张老师", row.get("teacherName"));
        // 打分中间量不应泄漏到接口返回里
        assertFalse(row.containsKey("simSum"));
        assertFalse(row.containsKey("bestSim"));
        assertFalse(row.containsKey("bestCourseName"));
    }
}
