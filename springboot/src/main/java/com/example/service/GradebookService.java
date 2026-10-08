package com.example.service;

import com.example.common.AppTime;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Assignment;
import com.example.entity.Course;
import com.example.entity.GradebookForm;
import com.example.entity.Homework;
import com.example.entity.Score;
import com.example.entity.Student;
import com.example.exception.CustomException;
import com.example.mapper.AssignmentMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.CourseSpaceMapper;
import com.example.mapper.ScoreMapper;
import com.example.mapper.StudentMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 成绩册：一门课全部选课学生的成绩，按「考勤 + 作业 + 平时 + 期末」四项加权出总评。
 *
 * <ul>
 *   <li>考勤分、作业分按记录自动折算（规则见 {@link GradeCalculator}），老师只录平时分和期末分；</li>
 *   <li>保存：写入权重和平时、期末分，并按当前的考勤、作业记录重算每一行。已发布的行保存后仍是已发布，
 *       及格状态变了学分跟着增减，总评有变动的学生会收到通知；</li>
 *   <li>发布：总评已算出的草稿行改成已发布，计入学分并通知学生；撤回：已发布的行退回草稿，扣回学分。</li>
 * </ul>
 *
 * <p>消息推送不在这里做：方法返回要通知的学生，由控制器在事务提交之后推送，
 * 避免事务回滚了学生却已经收到「成绩已发布」。</p>
 */
@Service
public class GradebookService {

    private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private CourseSpaceService courseSpaceService;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private ScoreMapper scoreMapper;
    @Resource
    private StudentMapper studentMapper;
    @Resource
    private CourseSpaceMapper courseSpaceMapper;
    @Resource
    private AssignmentMapper assignmentMapper;

    /** 保存、发布、撤回的结果 */
    public static class Outcome {
        /** 本次实际变动（或发布、撤回）的行数 */
        private final int count;
        /** 发布时因总评还没算出（待录）而跳过的学生数 */
        private final int skipped;
        /** 需要通知的学生 */
        private final List<Integer> notify;

        Outcome(int count, int skipped, List<Integer> notify) {
            this.count = count;
            this.skipped = skipped;
            this.notify = notify;
        }

        public int getCount() {
            return count;
        }

        public int getSkipped() {
            return skipped;
        }

        public List<Integer> getNotify() {
            return notify;
        }
    }

    /** 老师（或管理员）看的成绩册：权重、每个选课学生一行、汇总统计 */
    public Map<String, Object> view(Integer courseId) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireTeaching(course);
        int[] weights = GradeCalculator.weightsOf(course);

        List<Student> members = courseSpaceMapper.selectMembers(courseId);
        Map<Integer, Score> saved = byStudent(scoreMapper.selectByCourse(courseId));
        Map<Integer, Double[]> live = livePoints(courseId, idsOf(members));

        List<Map<String, Object>> rows = new ArrayList<>();
        List<Double> totals = new ArrayList<>();
        int published = 0;
        for (Student s : members) {
            Score row = saved.get(s.getId());
            Double[] points = live.get(s.getId());
            Double ordinary = row == null ? null : row.getOrdinaryScore();
            Double exam = row == null ? null : row.getExamScore();
            Double total = GradeCalculator.total(weights, points[0], points[1], ordinary, exam);
            boolean isPublished = row != null && isPublished(row);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("studentId", s.getId());
            item.put("username", s.getUsername());
            item.put("name", s.getName());
            item.put("avatar", s.getAvatar());
            item.put("className", s.getClassName());
            item.put("attendanceScore", points[0]);
            item.put("homeworkScore", points[1]);
            item.put("ordinaryScore", ordinary);
            item.put("examScore", exam);
            item.put("total", total);
            item.put("status", row == null ? null : statusOf(row));
            // 已发布的总评（学生现在看到的）；和上面实时算出的不一致时，前端提示「保存后更新」
            item.put("publishedTotal", isPublished ? row.getScore() : null);
            rows.add(item);

            if (total != null) {
                totals.add(total);
            }
            if (isPublished) {
                published++;
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("weights", weightMap(weights));
        data.put("courseStatus", course.getStatus());
        data.put("credit", course.getScore());
        data.put("rows", rows);
        data.put("stats", stats(members.size(), totals, published));
        return data;
    }

    /**
     * 学生看自己这门课的成绩：发布了给完整的四项和总评；没发布只给按记录实时折算的考勤分、作业分。
     */
    public Map<String, Object> mine(Integer courseId) {
        Course course = courseSpaceService.requireCourse(courseId);
        String relation = courseSpaceService.requireMember(course);
        if (!CourseSpaceService.REL_STUDENT.equals(relation)) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        Integer studentId = TokenUtils.getCurrentUser().getId();
        int[] weights = GradeCalculator.weightsOf(course);
        Score row = scoreMapper.selectByCourceIdAndStudentId(courseId, studentId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("weights", weightMap(weights));
        data.put("credit", course.getScore());
        boolean published = row != null && isPublished(row);
        data.put("published", published);
        if (published) {
            data.put("attendanceScore", row.getAttendanceScore());
            data.put("homeworkScore", row.getHomeworkScore());
            data.put("ordinaryScore", row.getOrdinaryScore());
            data.put("examScore", row.getExamScore());
            data.put("total", row.getScore());
            data.put("passed", row.getScore() != null && row.getScore() >= GradeCalculator.PASS_LINE);
            data.put("gradePoint", row.getScore() == null ? null : WorkbenchService.gradePoint(row.getScore()));
        } else {
            Double[] points = livePoints(courseId, Collections.singletonList(studentId)).get(studentId);
            data.put("attendanceScore", points[0]);
            data.put("homeworkScore", points[1]);
        }
        return data;
    }

    /** 保存成绩册：权重 + 学生的平时、期末分，并重算每一行 */
    @Transactional(rollbackFor = Exception.class)
    public Outcome save(Integer courseId, GradebookForm form) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireTeaching(course);

        int[] weights = form == null || form.getWeights() == null
                ? GradeCalculator.weightsOf(course) : weightArray(form.getWeights());
        if (!GradeCalculator.validWeights(weights)) {
            throw new CustomException(ResultCodeEnum.GRADE_WEIGHT_ERROR);
        }
        Map<Integer, GradebookForm.Row> inputs = new HashMap<>();
        if (form != null && form.getRows() != null) {
            for (GradebookForm.Row row : form.getRows()) {
                if (row == null || row.getStudentId() == null) {
                    continue;
                }
                if (!GradeCalculator.validComponent(row.getOrdinaryScore())
                        || !GradeCalculator.validComponent(row.getExamScore())) {
                    throw new CustomException(ResultCodeEnum.SCORE_RANGE_ERROR);
                }
                row.setOrdinaryScore(round2(row.getOrdinaryScore()));
                row.setExamScore(round2(row.getExamScore()));
                inputs.put(row.getStudentId(), row);
            }
        }

        if (!Arrays.equals(weights, GradeCalculator.weightsOf(course))) {
            Course patch = new Course();
            patch.setId(courseId);
            patch.setWeightAttendance(weights[GradeCalculator.ATTENDANCE]);
            patch.setWeightHomework(weights[GradeCalculator.HOMEWORK]);
            patch.setWeightOrdinary(weights[GradeCalculator.ORDINARY]);
            patch.setWeightExam(weights[GradeCalculator.EXAM]);
            courseMapper.updateById(patch);
            course.setWeightAttendance(patch.getWeightAttendance());
            course.setWeightHomework(patch.getWeightHomework());
            course.setWeightOrdinary(patch.getWeightOrdinary());
            course.setWeightExam(patch.getWeightExam());
        }
        return recompute(course, weights, inputs);
    }

    /** 发布：先按最新记录重算，再把总评已算出的草稿行全部发布 */
    @Transactional(rollbackFor = Exception.class)
    public Outcome publish(Integer courseId) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireTeaching(course);
        Outcome refreshed = recompute(course, GradeCalculator.weightsOf(course), Collections.emptyMap());

        Map<Integer, Score> saved = byStudent(scoreMapper.selectByCourse(courseId));
        List<Integer> notify = new ArrayList<>(refreshed.getNotify());
        int published = 0;
        int skipped = 0;
        for (Integer studentId : courseSpaceMapper.selectMemberIds(courseId)) {
            Score row = saved.get(studentId);
            if (row == null || row.getScore() == null) {
                skipped++;
                continue;
            }
            if (isPublished(row)) {
                continue;
            }
            row.setStatus(GradeCalculator.PUBLISHED);
            scoreMapper.updateGradebookRow(row);
            if (GradeCalculator.counted(GradeCalculator.PUBLISHED, row.getScore())) {
                adjustCredit(studentId, course, 1);
            }
            published++;
            if (!notify.contains(studentId)) {
                notify.add(studentId);
            }
        }
        return new Outcome(published, skipped, notify);
    }

    /** 撤回：这门课已发布的成绩全部退回草稿，及格的扣回学分 */
    @Transactional(rollbackFor = Exception.class)
    public Outcome unpublish(Integer courseId) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireTeaching(course);
        int count = 0;
        for (Score row : scoreMapper.selectByCourse(courseId)) {
            if (!isPublished(row)) {
                continue;
            }
            boolean wasCounted = GradeCalculator.counted(GradeCalculator.PUBLISHED, row.getScore());
            row.setStatus(GradeCalculator.DRAFT);
            scoreMapper.updateGradebookRow(row);
            if (wasCounted) {
                adjustCredit(row.getStudentId(), course, -1);
            }
            count++;
        }
        return new Outcome(count, 0, Collections.emptyList());
    }

    /**
     * 按记录实时折算的考勤分、作业分：studentId → [考勤分, 作业分]。
     * 一门课只查三次库（考勤按人汇总、作业任务、全部提交），在内存里按人拼。
     */
    public Map<Integer, Double[]> livePoints(Integer courseId, Collection<Integer> studentIds) {
        Map<Integer, Map<String, Long>> attendance = new HashMap<>();
        for (Map<String, Object> row : courseSpaceMapper.attendanceCountsByStudent(courseId)) {
            Integer studentId = toInt(row.get("studentId"));
            Object status = row.get("status");
            Object total = row.get("total");
            if (studentId == null || status == null || !(total instanceof Number)) {
                continue;
            }
            attendance.computeIfAbsent(studentId, k -> new HashMap<>())
                    .merge(String.valueOf(status), ((Number) total).longValue(), Long::sum);
        }

        List<Assignment> assignments = assignmentMapper.selectByCourse(courseId);
        Map<Integer, Map<Integer, Homework>> submissions = new HashMap<>();
        for (Homework h : courseSpaceMapper.selectSubmissionsOfCourse(courseId)) {
            if (h.getStudentId() != null && h.getAssignmentId() != null) {
                submissions.computeIfAbsent(h.getStudentId(), k -> new HashMap<>()).put(h.getAssignmentId(), h);
            }
        }

        String now = LocalDateTime.now(AppTime.clock()).format(MINUTES);
        Map<Integer, Double[]> result = new HashMap<>();
        for (Integer studentId : studentIds) {
            result.put(studentId, new Double[]{
                    GradeCalculator.attendancePoints(attendance.get(studentId)),
                    GradeCalculator.homeworkPoints(assignments, submissions.get(studentId), now),
            });
        }
        return result;
    }

    /**
     * 按当前权重和记录重算每个选课学生的成绩行。
     *
     * @param inputs 本次老师改过的平时、期末分（没出现的学生沿用库里的值）
     */
    private Outcome recompute(Course course, int[] weights, Map<Integer, GradebookForm.Row> inputs) {
        Integer courseId = course.getId();
        List<Integer> memberIds = courseSpaceMapper.selectMemberIds(courseId);
        Map<Integer, Score> saved = byStudent(scoreMapper.selectByCourse(courseId));
        Map<Integer, Double[]> live = livePoints(courseId, memberIds);

        int changed = 0;
        List<Integer> notify = new ArrayList<>();
        for (Integer studentId : memberIds) {
            Score row = saved.get(studentId);
            GradebookForm.Row input = inputs.get(studentId);
            Double ordinary = input != null ? input.getOrdinaryScore() : (row == null ? null : row.getOrdinaryScore());
            Double exam = input != null ? input.getExamScore() : (row == null ? null : row.getExamScore());
            Double[] points = live.get(studentId);
            Double total = GradeCalculator.total(weights, points[0], points[1], ordinary, exam);

            if (row == null) {
                // 什么都没录、总评也算不出来的学生不建空行
                if (ordinary == null && exam == null && total == null) {
                    continue;
                }
                Score fresh = new Score();
                fresh.setStudentId(studentId);
                fresh.setCourseId(courseId);
                fresh.setTeacherId(course.getTeacherId());
                fill(fresh, points, ordinary, exam, total);
                fresh.setStatus(GradeCalculator.DRAFT);
                scoreMapper.insertGradebookRow(fresh);
                changed++;
                continue;
            }

            String status = statusOf(row);
            // 已发布的成绩被清掉某一项、总评变回待录时退回草稿：学生不能看到一个没有总评的「已发布」成绩
            if (GradeCalculator.PUBLISHED.equals(status) && total == null) {
                status = GradeCalculator.DRAFT;
            }
            if (sameValues(row, points, ordinary, exam, total) && status.equals(row.getStatus())) {
                continue;
            }
            boolean wasCounted = GradeCalculator.counted(statusOf(row), row.getScore());
            Double before = row.getScore();
            fill(row, points, ordinary, exam, total);
            row.setStatus(status);
            scoreMapper.updateGradebookRow(row);
            changed++;

            boolean nowCounted = GradeCalculator.counted(status, total);
            if (wasCounted != nowCounted) {
                adjustCredit(studentId, course, nowCounted ? 1 : -1);
            }
            if (GradeCalculator.PUBLISHED.equals(status) && !Objects.equals(before, total)) {
                notify.add(studentId);
            }
        }
        return new Outcome(changed, 0, notify);
    }

    private static void fill(Score row, Double[] points, Double ordinary, Double exam, Double total) {
        row.setAttendanceScore(points[0]);
        row.setHomeworkScore(points[1]);
        row.setOrdinaryScore(ordinary);
        row.setExamScore(exam);
        row.setScore(total);
    }

    private static boolean sameValues(Score row, Double[] points, Double ordinary, Double exam, Double total) {
        return Objects.equals(row.getAttendanceScore(), points[0])
                && Objects.equals(row.getHomeworkScore(), points[1])
                && Objects.equals(row.getOrdinaryScore(), ordinary)
                && Objects.equals(row.getExamScore(), exam)
                && Objects.equals(row.getScore(), total);
    }

    /** 按课程学分增减学生的已修学分（SQL 原子自增） */
    private void adjustCredit(Integer studentId, Course course, int sign) {
        Integer credit = course.getScore();
        if (studentId == null || credit == null || credit == 0) {
            return;
        }
        studentMapper.addScore(studentId, sign * credit);
    }

    /** 状态为空的行按已发布算（与库里的默认值一致：改版前录入的成绩都是直接可见的） */
    static String statusOf(Score row) {
        return row.getStatus() == null ? GradeCalculator.PUBLISHED : row.getStatus();
    }

    static boolean isPublished(Score row) {
        return GradeCalculator.PUBLISHED.equals(statusOf(row));
    }

    static Map<String, Object> weightMap(int[] weights) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("attendance", weights[GradeCalculator.ATTENDANCE]);
        map.put("homework", weights[GradeCalculator.HOMEWORK]);
        map.put("ordinary", weights[GradeCalculator.ORDINARY]);
        map.put("exam", weights[GradeCalculator.EXAM]);
        return map;
    }

    private static int[] weightArray(GradebookForm.Weights w) {
        // 任何一项没传都按非法处理（-1 过不了 validWeights）
        return new int[]{
                w.getAttendance() == null ? -1 : w.getAttendance(),
                w.getHomework() == null ? -1 : w.getHomework(),
                w.getOrdinary() == null ? -1 : w.getOrdinary(),
                w.getExam() == null ? -1 : w.getExam(),
        };
    }

    /** 汇总：人数、已出总评、已发布、平均 / 最高 / 最低、及格率、五档分布（优、良、中、及格、不及格） */
    static Map<String, Object> stats(int students, List<Double> totals, int published) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("students", students);
        stats.put("graded", totals.size());
        stats.put("published", published);
        int[] distribution = new int[5];
        double sum = 0;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        int passed = 0;
        for (Double total : totals) {
            sum += total;
            max = Math.max(max, total);
            min = Math.min(min, total);
            if (total >= GradeCalculator.PASS_LINE) {
                passed++;
            }
            distribution[bucketOf(total)]++;
        }
        boolean empty = totals.isEmpty();
        stats.put("average", empty ? null : GradeCalculator.round1(sum / totals.size()));
        stats.put("highest", empty ? null : max);
        stats.put("lowest", empty ? null : min);
        stats.put("passRate", empty ? null : GradeCalculator.round1(passed * 100.0 / totals.size()));
        stats.put("distribution", distribution);
        return stats;
    }

    private static int bucketOf(double total) {
        if (total >= 90) {
            return 0;
        }
        if (total >= 80) {
            return 1;
        }
        if (total >= 70) {
            return 2;
        }
        if (total >= GradeCalculator.PASS_LINE) {
            return 3;
        }
        return 4;
    }

    private static Map<Integer, Score> byStudent(List<Score> rows) {
        Map<Integer, Score> map = new HashMap<>();
        for (Score row : rows) {
            if (row.getStudentId() != null) {
                // 历史数据里同一学生同一门课若有重复行，取 id 最大的（最近录入的）
                Score existing = map.get(row.getStudentId());
                if (existing == null || (row.getId() != null && existing.getId() != null && row.getId() > existing.getId())) {
                    map.put(row.getStudentId(), row);
                }
            }
        }
        return map;
    }

    private static List<Integer> idsOf(List<Student> students) {
        List<Integer> ids = new ArrayList<>(students.size());
        for (Student s : students) {
            ids.add(s.getId());
        }
        return ids;
    }

    private static Integer toInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    /** 与表里 double(10,2) 的精度一致，免得「存进去的」和「算总评用的」差一点点 */
    private static Double round2(Double value) {
        return value == null ? null : Math.round(value * 100) / 100.0;
    }
}
