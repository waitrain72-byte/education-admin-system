package com.example.service;

import com.example.entity.Choice;
import com.example.entity.Course;
import com.example.entity.Teacher;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.TeacherMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 课程推荐服务：基于物品的协同过滤（Item-Based Collaborative Filtering）。
 *
 * <p>原理：以 choice 选课记录构建「学生 × 课程」0/1 评分矩阵，课程间相似度采用余弦相似度：</p>
 * <pre>sim(A, B) = |U(A) ∩ U(B)| / sqrt(|U(A)| × |U(B)|)</pre>
 * <p>其中 U(X) 为选过课程 X 的学生集合。对目标学生未选择的每门候选课程 C 计算推荐得分：</p>
 * <pre>score(C) = Σ sim(C, s)（s 为该学生已选的课程）</pre>
 *
 * <p>取得分最高的 limit 门课程推荐，推荐理由取贡献最大的已选课程；
 * 当学生无选课记录或所有候选得分均为 0（冷启动）时，自动降级为按选课人数排序的热门推荐。
 * 已选课程不参与推荐。算法为纯内存计算，每次请求实时计算，无需预处理任务。</p>
 */
@Service
public class RecommendService {

    @Resource
    private ChoiceMapper choiceMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private TeacherMapper teacherMapper;

    /**
     * 为指定学生生成课程推荐列表
     *
     * @param studentId 学生ID（非学生账号传入时通常无选课记录，走热门推荐降级）
     * @param limit     推荐数量上限
     */
    public List<Map<String, Object>> recommendForStudent(Integer studentId, int limit) {
        // 1. 加载选课记录，构建「课程 → 选课学生集合」与「学生 → 已选课程集合」
        Map<Integer, Set<Integer>> courseUsers = new HashMap<>();
        Map<Integer, Set<Integer>> studentCourses = new HashMap<>();
        for (Choice ch : choiceMapper.selectAll(new Choice())) {
            if (ch.getCourseId() == null || ch.getStudentId() == null) {
                continue;
            }
            courseUsers.computeIfAbsent(ch.getCourseId(), k -> new HashSet<>()).add(ch.getStudentId());
            studentCourses.computeIfAbsent(ch.getStudentId(), k -> new HashSet<>()).add(ch.getCourseId());
        }

        // 2. 课程与教师名映射（推荐卡片展示用）
        Map<Integer, Course> courseMap = new HashMap<>();
        for (Course course : courseMapper.selectAll(new Course())) {
            courseMap.put(course.getId(), course);
        }
        Map<Integer, String> teacherNames = new HashMap<>();
        for (Teacher teacher : teacherMapper.selectAll(new Teacher())) {
            teacherNames.put(teacher.getId(), teacher.getName());
        }

        Set<Integer> myCourses = studentCourses.getOrDefault(studentId, Collections.emptySet());

        // 3. 候选课程打分：推荐得分 = 与各门已选课程余弦相似度之和
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Course course : courseMap.values()) {
            Integer cid = course.getId();
            if (myCourses.contains(cid)) {
                continue; // 已选课程不推荐
            }
            Set<Integer> users = courseUsers.getOrDefault(cid, Collections.emptySet());
            double simSum = 0;
            int bestCourseId = 0;
            double bestSim = 0;
            for (Integer selectedId : myCourses) {
                Set<Integer> otherUsers = courseUsers.getOrDefault(selectedId, Collections.emptySet());
                double denom = Math.sqrt((double) users.size() * otherUsers.size());
                double sim = denom == 0 ? 0 : intersectionSize(users, otherUsers) / denom;
                simSum += sim;
                if (sim > bestSim) {
                    bestSim = sim;
                    bestCourseId = selectedId;
                }
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", cid);
            row.put("name", course.getName());
            row.put("type", course.getType());
            row.put("credit", course.getScore());
            row.put("teacherName", teacherNames.getOrDefault(course.getTeacherId(), ""));
            row.put("popularity", users.size());
            row.put("simSum", simSum);
            if (bestCourseId != 0) {
                row.put("bestCourseName", courseMap.get(bestCourseId).getName());
            }
            row.put("bestSim", bestSim);
            candidates.add(row);
        }

        // 4. 排序：推荐得分优先，其次选课人数（热门程度）
        candidates.sort((a, b) -> {
            int bySim = Double.compare((double) b.get("simSum"), (double) a.get("simSum"));
            return bySim != 0 ? bySim : Integer.compare((int) b.get("popularity"), (int) a.get("popularity"));
        });

        // 5. 取前 limit 条生成推荐理由；无相似命中（冷启动）降级为热门理由
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : candidates) {
            if (result.size() >= limit) {
                break;
            }
            double bestSim = (double) row.get("bestSim");
            if (bestSim > 0) {
                row.put("reason", String.format("与已选《%s》相似度 %.0f%%", row.get("bestCourseName"), bestSim * 100));
            } else {
                row.put("reason", row.get("popularity") + " 人已选");
            }
            row.remove("simSum");
            row.remove("bestSim");
            row.remove("bestCourseName");
            result.add(row);
        }
        return result;
    }

    private int intersectionSize(Set<Integer> a, Set<Integer> b) {
        int n = 0;
        for (Integer x : a) {
            if (b.contains(x)) {
                n++;
            }
        }
        return n;
    }
}
