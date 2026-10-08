package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.AppTime;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Assignment;
import com.example.entity.AttendanceSession;
import com.example.entity.Course;
import com.example.entity.Homework;
import com.example.entity.Notice;
import com.example.entity.Student;
import com.example.entity.Teacher;
import com.example.exception.CustomException;
import com.example.mapper.AssignmentMapper;
import com.example.mapper.AttendanceSessionMapper;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.CourseSpaceMapper;
import com.example.mapper.NoticeMapper;
import com.example.mapper.TeacherMapper;
import com.example.utils.TokenUtils;
import com.example.websocket.NoticeWebSocketServer;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 课程空间：以「一门课」为单位的数据入口。
 *
 * <p>当前登录人与这门课的关系决定能看到什么：</p>
 * <ul>
 *   <li>admin —— 管理员，全部可见；</li>
 *   <li>teacher —— 这门课的任课教师；</li>
 *   <li>student —— 选了这门课的学生；</li>
 *   <li>visitor —— 其他人（如还没选课的学生在课程广场里预览），只能看课程基本信息。</li>
 * </ul>
 */
@Service
public class CourseSpaceService {

    public static final String REL_ADMIN = "admin";
    public static final String REL_TEACHER = "teacher";
    public static final String REL_STUDENT = "student";
    public static final String REL_VISITOR = "visitor";

    static final int MAX_INTRO = 500;
    static final int MAX_POST_TITLE = 100;
    static final int MAX_POST_CONTENT = 2000;
    /** 公告列表最多返回的条数 */
    static final int MAX_POSTS = 50;

    private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private CourseMapper courseMapper;
    @Resource
    private ChoiceMapper choiceMapper;
    @Resource
    private TeacherMapper teacherMapper;
    @Resource
    private CourseSpaceMapper courseSpaceMapper;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private AssignmentMapper assignmentMapper;
    @Resource
    private AttendanceSessionMapper attendanceSessionMapper;
    @Resource
    private MessageService messageService;
    @Resource
    private EnrollmentService enrollmentService;

    /** 课程（带任课教师姓名）；不存在时报参数错误 */
    public Course requireCourse(Integer courseId) {
        if (courseId == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        Course probe = new Course();
        probe.setId(courseId);
        List<Course> list = courseMapper.selectAll(probe);
        if (list.isEmpty()) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        return list.get(0);
    }

    /** 当前登录人与课程的关系 */
    public String relationOf(Course course) {
        Account current = TokenUtils.getCurrentUser();
        return relationOf(course, current);
    }

    String relationOf(Course course, Account current) {
        String role = current.getRole();
        if (RoleEnum.ADMIN.name().equals(role)) {
            return REL_ADMIN;
        }
        if (RoleEnum.TEACHER.name().equals(role) && current.getId() != null
                && current.getId().equals(course.getTeacherId())) {
            return REL_TEACHER;
        }
        if (RoleEnum.STUDENT.name().equals(role) && current.getId() != null
                && choiceMapper.countByStudentAndCourse(current.getId(), course.getId()) > 0) {
            return REL_STUDENT;
        }
        return REL_VISITOR;
    }

    /** 课程成员才能进入（管理员、任课教师、选课学生），否则 403 */
    public String requireMember(Course course) {
        String relation = relationOf(course);
        if (REL_VISITOR.equals(relation)) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        return relation;
    }

    /** 只有任课教师和管理员能做（布置作业、发起签到、录成绩……） */
    public void requireTeaching(Course course) {
        String relation = relationOf(course);
        if (!REL_TEACHER.equals(relation) && !REL_ADMIN.equals(relation)) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
    }

    /**
     * 课程概览：课程信息、任课教师、选课人数、当前登录人与课程的关系、简介、总评权重，
     * 以及概览页的提醒：是否正在签到；学生还有几份作业没交，老师还有几份作业没批。
     */
    public Map<String, Object> overview(Integer courseId) {
        Course course = requireCourse(courseId);
        String relation = relationOf(course);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("course", WorkbenchService.courseCard(course.getId(), course.getName(), course.getType(),
                course.getScore(), course.getTeacherName(), course.getRoom(), course.getWeek(), course.getSegment(),
                course.getStatus(), course.getNum()));
        data.put("teacher", teacherBrief(course.getTeacherId()));
        data.put("studentCount", choiceMapper.countByCourseId(courseId));
        data.put("relation", relation);
        data.put("intro", course.getIntro());
        data.put("weights", GradebookService.weightMap(GradeCalculator.weightsOf(course)));
        Account me = TokenUtils.getCurrentUser();
        if (REL_VISITOR.equals(relation)) {
            // 没选这门课的学生在概览里直接选：给出能不能选、剩几个名额、和哪门课冲突
            if (RoleEnum.STUDENT.name().equals(me.getRole())) {
                int count = choiceMapper.countByCourseId(courseId);
                Map<String, Object> enroll = new LinkedHashMap<>();
                enroll.put("ended", EnrollmentService.FINISHED.equals(course.getStatus()));
                enroll.put("seatsLeft", course.getNum() == null ? null : Math.max(0, course.getNum() - count));
                Course clash = EnrollmentService.conflictOf(course, choiceMapper.selectActiveSlotsByStudentId(me.getId()));
                enroll.put("conflict", clash == null ? null : clash.getName());
                data.put("enroll", enroll);
            }
            return data;
        }
        if (REL_STUDENT.equals(relation)) {
            data.put("canDrop", enrollmentService.canDrop(me.getId(), course));
        }

        data.put("signing", isSigning(courseId));
        List<Assignment> assignments = assignmentMapper.selectByCourse(courseId);
        List<Homework> submissions = courseSpaceMapper.selectSubmissionsOfCourse(courseId);
        String now = LocalDateTime.now(AppTime.clock()).format(MINUTES);
        if (REL_STUDENT.equals(relation)) {
            Set<Integer> done = new HashSet<>();
            for (Homework h : submissions) {
                if (me.getId().equals(h.getStudentId())) {
                    done.add(h.getAssignmentId());
                }
            }
            long pending = assignments.stream()
                    .filter(a -> !done.contains(a.getId()) && !GradeCalculator.isPast(a.getDeadline(), now))
                    .count();
            data.put("pendingAssignments", pending);
        } else {
            long ungraded = submissions.stream().filter(h -> !AssignmentService.GRADED.equals(h.getStatus())).count();
            // 旧版直接上传、还没打分的也算待批改（与首页「待批改」同一口径：score 为空）
            ungraded += courseSpaceMapper.selectLooseSubmissions(courseId).stream().filter(h -> h.getScore() == null).count();
            data.put("ungraded", ungraded);
        }
        data.put("assignmentCount", assignments.size());
        return data;
    }

    /**
     * 成员（花名册）：老师和管理员看到学号和每个学生的学习情况（出勤率、作业完成数）；
     * 学生只看到同学的姓名、头像、班级。
     */
    public Map<String, Object> members(Integer courseId) {
        Course course = requireCourse(courseId);
        String relation = requireMember(course);
        boolean teaching = !REL_STUDENT.equals(relation);
        List<Student> students = courseSpaceMapper.selectMembers(courseId);

        Map<Integer, Map<String, Long>> attendance = new HashMap<>();
        Map<Integer, Long> submitted = new HashMap<>();
        int assignmentCount = 0;
        if (teaching) {
            for (Map<String, Object> row : courseSpaceMapper.attendanceCountsByStudent(courseId)) {
                Object sid = row.get("studentId");
                Object total = row.get("total");
                if (sid instanceof Number && total instanceof Number) {
                    attendance.computeIfAbsent(((Number) sid).intValue(), k -> new HashMap<>())
                            .merge(String.valueOf(row.get("status")), ((Number) total).longValue(), Long::sum);
                }
            }
            for (Homework h : courseSpaceMapper.selectSubmissionsOfCourse(courseId)) {
                submitted.merge(h.getStudentId(), 1L, Long::sum);
            }
            assignmentCount = assignmentMapper.selectByCourse(courseId).size();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Student s : students) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", s.getId());
            row.put("name", s.getName());
            row.put("avatar", s.getAvatar());
            row.put("className", s.getClassName());
            if (teaching) {
                row.put("username", s.getUsername());
                row.put("attendanceRate", attendanceRate(attendance.get(s.getId())));
                row.put("submitted", submitted.getOrDefault(s.getId(), 0L));
            }
            rows.add(row);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("teacher", teacherBrief(course.getTeacherId()));
        data.put("students", rows);
        if (teaching) {
            data.put("assignmentCount", assignmentCount);
        }
        return data;
    }

    /** 课程公告（新的在前） */
    public List<Notice> posts(Integer courseId) {
        Course course = requireCourse(courseId);
        requireMember(course);
        Notice probe = new Notice();
        probe.setCourseId(courseId);
        PageHelper.startPage(1, MAX_POSTS, false);
        return noticeMapper.selectAll(probe);
    }

    /** 发布课程公告（任课教师、管理员）；通知由控制器在事务提交后推送 */
    @Transactional(rollbackFor = Exception.class)
    public Notice addPost(Integer courseId, Notice form) {
        Course course = requireCourse(courseId);
        requireTeaching(course);
        String title = form == null ? null : StrUtil.trimToNull(form.getTitle());
        String content = form == null ? null : StrUtil.trimToNull(form.getContent());
        if (title == null || content == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (title.length() > MAX_POST_TITLE || content.length() > MAX_POST_CONTENT) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Notice post = new Notice();
        post.setCourseId(courseId);
        post.setTitle(title);
        post.setContent(content);
        post.setTime(LocalDateTime.now(AppTime.clock()).format(MINUTES));
        post.setUser(TokenUtils.getCurrentUser().getName());
        noticeMapper.insert(post);
        return post;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Integer courseId, Integer postId) {
        Course course = requireCourse(courseId);
        requireTeaching(course);
        Notice post = postId == null ? null : noticeMapper.selectById(postId);
        if (post == null || !courseId.equals(post.getCourseId())) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        noticeMapper.deleteById(postId);
    }

    /** 修改课程简介（任课教师、管理员），传空即清空 */
    @Transactional(rollbackFor = Exception.class)
    public String updateIntro(Integer courseId, String intro) {
        Course course = requireCourse(courseId);
        requireTeaching(course);
        String text = StrUtil.trimToEmpty(intro);
        if (text.length() > MAX_INTRO) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Course patch = new Course();
        patch.setId(courseId);
        patch.setIntro(text);
        courseMapper.updateById(patch);
        return text;
    }

    /** 这门课现在是否有进行中（没过截止时间）的签到 */
    public boolean isSigning(Integer courseId) {
        AttendanceSession session = attendanceSessionMapper.selectActiveByCourse(courseId);
        return session != null && session.getExpireTime() != null
                && session.getExpireTime().compareTo(LocalDateTime.now(AppTime.clock()).format(SECONDS)) >= 0;
    }

    /** 给全体选课学生发一条站内消息（落库 + 实时推送） */
    public void notifyStudents(Integer courseId, String type, String title, String content, String link) {
        for (Integer studentId : courseSpaceMapper.selectMemberIds(courseId)) {
            messageService.push(studentId, RoleEnum.STUDENT.name(), type, title, content, link);
        }
    }

    /**
     * 给课程成员（选课学生 + 任课教师）推一条静默事件：正在看课程空间的页面据此刷新数据。
     * 不落库、不弹通知（没有 title），小程序收到会直接忽略。
     */
    public void emit(Integer courseId, String event) {
        Map<String, Object> payload = eventPayload(courseId, event);
        for (Integer studentId : courseSpaceMapper.selectMemberIds(courseId)) {
            NoticeWebSocketServer.sendPayload(studentId, RoleEnum.STUDENT.name(), payload);
        }
        emitToTeacher(courseId, event);
    }

    /** 只推给任课教师的静默事件（如学生签到后老师端的人数刷新） */
    public void emitToTeacher(Integer courseId, String event) {
        Course course = courseMapper.selectById(courseId);
        if (course != null && course.getTeacherId() != null) {
            NoticeWebSocketServer.sendPayload(course.getTeacherId(), RoleEnum.TEACHER.name(), eventPayload(courseId, event));
        }
    }

    private static Map<String, Object> eventPayload(Integer courseId, String event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "course");
        payload.put("event", event);
        payload.put("courseId", courseId);
        return payload;
    }

    /** 出勤率（百分比，一位小数）：与首页口径一致——迟到、早退算出勤，请假不计入分母；没有记录返回 null */
    static Double attendanceRate(Map<String, Long> counts) {
        if (counts == null) {
            return null;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", entry.getKey());
            row.put("value", entry.getValue());
            rows.add(row);
        }
        return WorkbenchService.attendanceRate(rows);
    }

    /** 任课教师的公开信息：姓名、职称、头像、邮箱（不含电话等私人信息） */
    private Map<String, Object> teacherBrief(Integer teacherId) {
        if (teacherId == null) {
            return null;
        }
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            return null;
        }
        Map<String, Object> brief = new LinkedHashMap<>();
        brief.put("id", teacher.getId());
        brief.put("name", teacher.getName());
        brief.put("title", teacher.getTitle());
        brief.put("avatar", teacher.getAvatar());
        brief.put("email", teacher.getEmail());
        return brief;
    }
}
