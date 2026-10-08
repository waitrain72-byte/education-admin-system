package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.AppTime;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Assignment;
import com.example.entity.Course;
import com.example.entity.Homework;
import com.example.entity.Student;
import com.example.exception.CustomException;
import com.example.mapper.AssignmentMapper;
import com.example.mapper.CourseSpaceMapper;
import com.example.mapper.HomeworkMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 作业任务：老师布置（标题、要求、附件、截止时间、满分），学生在截止前提交（文字和 / 或附件），老师批改打分。
 *
 * <ul>
 *   <li>提交存在 homework 表（assignment_id 指向作业任务），与旧版「学生直接上传」的记录共表；</li>
 *   <li>截止后不能再交；批改后不能再改；截止前、批改前可以反复修改提交；</li>
 *   <li>学生看到的状态：未提交 / 已提交 / 已批改 / 已逾期（过了截止还没交）。</li>
 * </ul>
 */
@Service
public class AssignmentService {

    public static final String SUBMITTED = "已提交";
    public static final String GRADED = "已批改";
    /** 学生视角的派生状态 */
    public static final String PENDING = "未提交";
    public static final String OVERDUE = "已逾期";

    static final int MAX_TITLE = 100;
    static final int MAX_CONTENT = 5000;
    static final int MAX_FULL_SCORE = 1000;
    /** 旧版自由提交的记录没有作业任务，按百分制批改 */
    static final int LOOSE_FULL_SCORE = 100;

    private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private CourseSpaceService courseSpaceService;
    @Resource
    private AssignmentMapper assignmentMapper;
    @Resource
    private HomeworkMapper homeworkMapper;
    @Resource
    private CourseSpaceMapper courseSpaceMapper;
    @Resource
    private UploadedFiles uploadedFiles;

    /** 作业列表：老师看每个作业的提交、批改人数；学生看每个作业自己的状态 */
    public Map<String, Object> list(Integer courseId) {
        Course course = courseSpaceService.requireCourse(courseId);
        String relation = courseSpaceService.requireMember(course);
        List<Assignment> assignments = assignmentMapper.selectByCourse(courseId);
        String now = now();
        boolean student = CourseSpaceService.REL_STUDENT.equals(relation);

        Map<Integer, List<Homework>> byAssignment = new HashMap<>();
        Map<Integer, Homework> mine = new HashMap<>();
        Integer me = TokenUtils.getCurrentUser().getId();
        for (Homework h : courseSpaceMapper.selectSubmissionsOfCourse(courseId)) {
            byAssignment.computeIfAbsent(h.getAssignmentId(), k -> new ArrayList<>()).add(h);
            if (student && me != null && me.equals(h.getStudentId())) {
                mine.put(h.getAssignmentId(), h);
            }
        }
        int memberCount = courseSpaceMapper.selectMemberIds(courseId).size();

        List<Map<String, Object>> items = new ArrayList<>();
        for (Assignment a : assignments) {
            Map<String, Object> item = brief(a, now);
            if (student) {
                Homework h = mine.get(a.getId());
                item.put("myStatus", studentStatus(h, a, now));
                item.put("myScore", h == null || !GRADED.equals(h.getStatus()) ? null : h.getScore());
            } else {
                List<Homework> subs = byAssignment.getOrDefault(a.getId(), new ArrayList<>());
                item.put("submitted", subs.size());
                item.put("graded", subs.stream().filter(h -> GRADED.equals(h.getStatus())).count());
                item.put("students", memberCount);
            }
            items.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("now", now);
        data.put("assignments", items);
        if (!student) {
            data.put("looseCount", courseSpaceMapper.selectLooseSubmissions(courseId).size());
        }
        return data;
    }

    /** 作业详情：老师看全班名单和每人的提交；学生看自己的提交 */
    public Map<String, Object> detail(Integer courseId, Integer assignmentId) {
        Course course = courseSpaceService.requireCourse(courseId);
        String relation = courseSpaceService.requireMember(course);
        Assignment assignment = requireAssignment(courseId, assignmentId);
        String now = now();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("now", now);
        data.put("assignment", brief(assignment, now));
        if (CourseSpaceService.REL_STUDENT.equals(relation)) {
            Homework mine = courseSpaceMapper.selectSubmission(assignmentId, TokenUtils.getCurrentUser().getId());
            data.put("mySubmission", mine);
            data.put("myStatus", studentStatus(mine, assignment, now));
            return data;
        }

        Map<Integer, Homework> byStudent = new HashMap<>();
        for (Homework h : courseSpaceMapper.selectSubmissionsOfAssignment(assignmentId)) {
            byStudent.put(h.getStudentId(), h);
        }
        List<Map<String, Object>> roster = new ArrayList<>();
        for (Student s : courseSpaceMapper.selectMembers(courseId)) {
            Homework h = byStudent.get(s.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", s.getId());
            row.put("username", s.getUsername());
            row.put("name", s.getName());
            row.put("avatar", s.getAvatar());
            row.put("className", s.getClassName());
            row.put("status", studentStatus(h, assignment, now));
            row.put("submission", h);
            roster.add(row);
        }
        data.put("roster", roster);
        return data;
    }

    /** 旧版「学生直接上传」、没挂在作业任务下的提交（老师可在课程里接着批改） */
    public List<Homework> looseSubmissions(Integer courseId) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireTeaching(course);
        return courseSpaceMapper.selectLooseSubmissions(courseId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Assignment create(Integer courseId, Assignment form) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireTeaching(course);
        Assignment assignment = new Assignment();
        assignment.setCourseId(courseId);
        assignment.setTeacherId(course.getTeacherId());
        assignment.setCreateTime(now());
        apply(assignment, form);
        uploadedFiles.require(assignment.getAttachment());
        assignmentMapper.insert(assignment);
        return assignment;
    }

    @Transactional(rollbackFor = Exception.class)
    public Assignment update(Integer courseId, Integer assignmentId, Assignment form) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireTeaching(course);
        Assignment assignment = requireAssignment(courseId, assignmentId);
        apply(assignment, form);
        uploadedFiles.require(assignment.getAttachment());
        assignmentMapper.update(assignment);
        return assignment;
    }

    /** 删除作业任务，连同学生的提交 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer courseId, Integer assignmentId) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireTeaching(course);
        requireAssignment(courseId, assignmentId);
        courseSpaceMapper.deleteSubmissionsOfAssignment(assignmentId);
        assignmentMapper.deleteById(assignmentId);
    }

    /** 学生提交（或在截止前、批改前修改提交） */
    @Transactional(rollbackFor = Exception.class)
    public Homework submit(Integer courseId, Integer assignmentId, Homework form) {
        Course course = courseSpaceService.requireCourse(courseId);
        if (!CourseSpaceService.REL_STUDENT.equals(courseSpaceService.relationOf(course))) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        Assignment assignment = requireAssignment(courseId, assignmentId);
        String now = now();
        if (GradeCalculator.isPast(assignment.getDeadline(), now)) {
            throw new CustomException(ResultCodeEnum.ASSIGNMENT_CLOSED_ERROR);
        }
        String content = form == null ? null : StrUtil.trimToNull(form.getContent());
        String file = uploadedFiles.require(form == null ? null : StrUtil.trimToNull(form.getFile()));
        if (content == null && file == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (content != null && content.length() > MAX_CONTENT) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        String fileName = file == null ? null : StrUtil.maxLength(StrUtil.blankToDefault(form.getFileName(), "附件"), 200);

        Integer studentId = TokenUtils.getCurrentUser().getId();
        Homework existing = courseSpaceMapper.selectSubmission(assignmentId, studentId);
        if (existing != null) {
            if (GRADED.equals(existing.getStatus())) {
                throw new CustomException(ResultCodeEnum.ASSIGNMENT_GRADED_ERROR);
            }
            // 改提交要能清掉旧附件或旧文字，所以两项都按本次的值写（空就是去掉）
            existing.setContent(content == null ? "" : content);
            existing.setFile(file == null ? "" : file);
            existing.setFileName(fileName == null ? "" : fileName);
            existing.setSubmitTime(now);
            existing.setStatus(SUBMITTED);
            homeworkMapper.updateById(existing);
            return existing;
        }
        Homework submission = new Homework();
        submission.setAssignmentId(assignmentId);
        submission.setCourseId(courseId);
        submission.setStudentId(studentId);
        submission.setTeacherId(course.getTeacherId());
        submission.setContent(content);
        submission.setFile(file);
        submission.setFileName(fileName);
        submission.setSubmitTime(now);
        submission.setStatus(SUBMITTED);
        homeworkMapper.insert(submission);
        return submission;
    }

    /**
     * 批改：分数在 0 ~ 满分之间（旧版自由提交按百分制），可附评语；批改过的可以重新打分。
     *
     * @return 批改后的提交（控制器据此通知学生）
     */
    @Transactional(rollbackFor = Exception.class)
    public Homework grade(Integer courseId, Integer homeworkId, Double score, String comment) {
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.requireTeaching(course);
        Homework submission = homeworkId == null ? null : homeworkMapper.selectById(homeworkId);
        if (submission == null || !courseId.equals(submission.getCourseId())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        int full = LOOSE_FULL_SCORE;
        if (submission.getAssignmentId() != null) {
            Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
            if (assignment != null && assignment.getFullScore() != null && assignment.getFullScore() > 0) {
                full = assignment.getFullScore();
            }
        }
        if (score == null || score.isNaN() || score < 0 || score > full) {
            throw new CustomException(ResultCodeEnum.HOMEWORK_SCORE_ERROR);
        }
        String text = StrUtil.maxLength(StrUtil.trimToEmpty(comment), 500);
        submission.setScore(format(score));
        submission.setDescr(text);
        submission.setStatus(GRADED);
        homeworkMapper.updateById(submission);
        return submission;
    }

    public Assignment requireAssignment(Integer courseId, Integer assignmentId) {
        Assignment assignment = assignmentId == null ? null : assignmentMapper.selectById(assignmentId);
        if (assignment == null || !courseId.equals(assignment.getCourseId())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        return assignment;
    }

    /** 学生视角的状态：已批改 / 已提交 / 已逾期 / 未提交 */
    static String studentStatus(Homework submission, Assignment assignment, String now) {
        if (submission != null) {
            return GRADED.equals(submission.getStatus()) ? GRADED : SUBMITTED;
        }
        return GradeCalculator.isPast(assignment.getDeadline(), now) ? OVERDUE : PENDING;
    }

    /** 表单校验后写入：标题必填、截止时间必填（yyyy-MM-dd HH:mm）、满分 1~1000（默认 100） */
    private static void apply(Assignment target, Assignment form) {
        if (form == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        String title = StrUtil.trimToNull(form.getTitle());
        String deadline = StrUtil.trimToNull(form.getDeadline());
        if (title == null || deadline == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (title.length() > MAX_TITLE || !isMinuteTime(deadline)
                || (form.getContent() != null && form.getContent().length() > MAX_CONTENT)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        int full = form.getFullScore() == null ? 100 : form.getFullScore();
        if (full < 1 || full > MAX_FULL_SCORE) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        String attachment = StrUtil.trimToNull(form.getAttachment());
        target.setTitle(title);
        target.setContent(StrUtil.trimToEmpty(form.getContent()));
        target.setDeadline(deadline);
        target.setFullScore(full);
        target.setAttachment(attachment);
        target.setAttachmentName(attachment == null ? null
                : StrUtil.maxLength(StrUtil.blankToDefault(form.getAttachmentName(), "附件"), 200));
    }

    private static Map<String, Object> brief(Assignment a, String now) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", a.getId());
        item.put("title", a.getTitle());
        item.put("content", a.getContent());
        item.put("attachment", a.getAttachment());
        item.put("attachmentName", a.getAttachmentName());
        item.put("deadline", a.getDeadline());
        item.put("fullScore", a.getFullScore());
        item.put("createTime", a.getCreateTime());
        item.put("closed", GradeCalculator.isPast(a.getDeadline(), now));
        return item;
    }

    /** 分数存成字符串（homework.score 是 varchar）：整数不带小数点，其余保留一位 */
    static String format(double score) {
        double rounded = GradeCalculator.round1(score);
        return rounded == Math.rint(rounded) ? String.valueOf((long) rounded) : String.valueOf(rounded);
    }

    static boolean isMinuteTime(String value) {
        if (value == null || value.length() != 16) {
            return false;
        }
        try {
            LocalDateTime.parse(value, MINUTES);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static String now() {
        return LocalDateTime.now(AppTime.clock()).format(MINUTES);
    }
}
