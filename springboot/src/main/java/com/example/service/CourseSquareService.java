package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.entity.Score;
import com.example.exception.CustomException;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.ScoreMapper;
import com.example.mapper.WorkbenchMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 课程广场：浏览、筛选全部课程，学生在这里选课、退选。
 *
 * <p>每张卡片带上选课人数与剩余名额；学生看到的卡片还标出「已选」、和自己已选课程的时间冲突，
 * 以及基于协同过滤的推荐理由（{@link RecommendService}）。</p>
 */
@Service
public class CourseSquareService {

    static final String FINISHED = "已结课";
    /** 简介在卡片上最多显示的字数 */
    static final int INTRO_PREVIEW = 60;
    static final int RECOMMEND_LIMIT = 6;

    @Resource
    private CourseMapper courseMapper;
    @Resource
    private ChoiceMapper choiceMapper;
    @Resource
    private WorkbenchMapper workbenchMapper;
    @Resource
    private RecommendService recommendService;
    @Resource
    private EnrollmentService enrollmentService;
    @Resource
    private ScoreMapper scoreMapper;

    /** 筛选条件；都可以为空 */
    public static class Query {
        public String keyword;
        public String type;
        public String week;
        public String status;
        /** 只看还有名额的 */
        public boolean available;
        /** 默认不显示已结课的课 */
        public boolean includeEnded;
    }

    public Map<String, Object> square(Query query) {
        Account me = TokenUtils.getCurrentUser();
        boolean student = RoleEnum.STUDENT.name().equals(me.getRole());
        List<Course> all = courseMapper.selectAll(new Course());
        Map<Integer, Long> enrolled = countMap(workbenchMapper.countStudentsAll());
        Set<Integer> mine = student ? new HashSet<>(choiceMapper.selectCourseIdsByStudentId(me.getId())) : Collections.emptySet();
        // 成绩已发布的课不能退选：卡片上就不给「退选」按钮
        Set<Integer> published = new HashSet<>();
        if (student) {
            Score probe = new Score();
            probe.setStudentId(me.getId());
            probe.setStatus(GradeCalculator.PUBLISHED);
            for (Score s : scoreMapper.selectAll(probe)) {
                published.add(s.getCourseId());
            }
        }
        List<Course> mySlots = student ? choiceMapper.selectActiveSlotsByStudentId(me.getId()) : Collections.emptyList();
        List<Map<String, Object>> recs = student
                ? recommendService.recommendForStudent(me.getId(), RECOMMEND_LIMIT) : Collections.emptyList();
        Map<Integer, String> reasons = new HashMap<>();
        if (student) {
            for (Map<String, Object> r : recs) {
                Object id = r.get("id");
                // 既不相似、也没人选的课（冷启动兜底时会排进来）算不上推荐
                if (id instanceof Integer && worthRecommending(r)) {
                    reasons.put((Integer) id, String.valueOf(r.get("reason")));
                }
            }
        }

        Set<String> types = new LinkedHashSet<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Course c : all) {
            if (StrUtil.isNotBlank(c.getType())) {
                types.add(c.getType());
            }
            if (!matches(c, query)) {
                continue;
            }
            long count = enrolled.getOrDefault(c.getId(), 0L);
            Integer seatsLeft = c.getNum() == null ? null : (int) Math.max(0, c.getNum() - count);
            if (query.available && seatsLeft != null && seatsLeft <= 0) {
                continue;
            }
            Map<String, Object> card = card(c, count, seatsLeft);
            if (student) {
                boolean isMine = mine.contains(c.getId());
                card.put("enrolled", isMine);
                card.put("canDrop", isMine && !FINISHED.equals(c.getStatus()) && !published.contains(c.getId()));
                if (!isMine) {
                    Course clash = EnrollmentService.conflictOf(c, mySlots);
                    card.put("conflict", clash == null ? null : clash.getName());
                }
                card.put("reason", isMine ? null : reasons.get(c.getId()));
            }
            rows.add(card);
        }
        // 正在开的课在前，已结课的在最后；同一状态按课程名
        rows.sort(Comparator.comparingInt((Map<String, Object> r) -> FINISHED.equals(r.get("status")) ? 1 : 0)
                .thenComparing(r -> String.valueOf(r.get("name"))));

        List<Map<String, Object>> recommendations = new ArrayList<>();
        if (student) {
            for (Map<String, Object> r : recs) {
                Object id = r.get("id");
                if (!worthRecommending(r)) {
                    continue;
                }
                for (Map<String, Object> row : rows) {
                    if (row.get("id").equals(id) && !FINISHED.equals(row.get("status")) && !Boolean.TRUE.equals(row.get("enrolled"))) {
                        recommendations.add(row);
                    }
                }
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("courses", rows);
        data.put("recommendations", recommendations.size() > 4 ? recommendations.subList(0, 4) : recommendations);
        data.put("types", types);
        data.put("canEnroll", student);
        return data;
    }

    /** 学生选课；返回选上的课程卡片 */
    public Map<String, Object> enroll(Integer courseId) {
        Account me = requireStudent();
        Course course = enrollmentService.enroll(me.getId(), courseId);
        return card(course, choiceMapper.countByCourseId(courseId), null);
    }

    /** 学生退选 */
    public void drop(Integer courseId) {
        Account me = requireStudent();
        enrollmentService.drop(me.getId(), courseId);
    }

    /** 推荐得有理由：和已选课程相似，或至少有人选过 */
    static boolean worthRecommending(Map<String, Object> rec) {
        Object popularity = rec.get("popularity");
        String reason = String.valueOf(rec.get("reason"));
        return reason.contains("相似度") || (popularity instanceof Number && ((Number) popularity).intValue() > 0);
    }

    private static Account requireStudent() {
        Account me = TokenUtils.getCurrentUser();
        if (!RoleEnum.STUDENT.name().equals(me.getRole())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        return me;
    }

    static boolean matches(Course c, Query q) {
        if (!q.includeEnded && FINISHED.equals(c.getStatus())) {
            return false;
        }
        if (StrUtil.isNotBlank(q.type) && !q.type.equals(c.getType())) {
            return false;
        }
        if (StrUtil.isNotBlank(q.week) && !q.week.equals(c.getWeek())) {
            return false;
        }
        if (StrUtil.isNotBlank(q.status) && !q.status.equals(c.getStatus())) {
            return false;
        }
        if (StrUtil.isNotBlank(q.keyword)) {
            String k = q.keyword.trim().toLowerCase();
            return contains(c.getName(), k) || contains(c.getTeacherName(), k) || contains(c.getRoom(), k);
        }
        return true;
    }

    private static boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase().contains(keyword);
    }

    private static Map<String, Object> card(Course c, long count, Integer seatsLeft) {
        Map<String, Object> card = WorkbenchService.courseCard(c.getId(), c.getName(), c.getType(), c.getScore(),
                c.getTeacherName(), c.getRoom(), c.getWeek(), c.getSegment(), c.getStatus(), c.getNum());
        card.put("studentCount", count);
        card.put("seatsLeft", seatsLeft);
        String intro = StrUtil.trimToNull(c.getIntro());
        card.put("intro", intro == null ? null : StrUtil.maxLength(intro.replaceAll("\\s+", " "), INTRO_PREVIEW));
        return card;
    }

    private static Map<Integer, Long> countMap(List<Map<String, Object>> rows) {
        Map<Integer, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get("courseId");
            Object total = row.get("total");
            if (id instanceof Number && total instanceof Number) {
                map.put(((Number) id).intValue(), ((Number) total).longValue());
            }
        }
        return map;
    }
}
