package com.example.service;

import com.example.common.AppTime;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.entity.Homework;
import com.example.exception.CustomException;
import com.example.mapper.ChoiceMapper;
import com.example.mapper.CourseMapper;
import com.example.mapper.CrudMapper;
import com.example.mapper.HomeworkMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 作业信息表业务处理（通用增删改查见 {@link CrudService}），旧版作业页和小程序在用。
 *
 * <p>课程空间改版后作业提交按作业任务走 {@link AssignmentService}，它们的得分会算进成绩册，
 * 所以这里的旧接口要守住同样的规矩：</p>
 * <ul>
 *   <li>学生只能以自己的名义、给自己选了的课交「直接上传」的作业，分数、评语、状态、所属作业任务一概不收；</li>
 *   <li>学生只能改、删自己还没批改的直接上传作业，而且只能改内容和附件；</li>
 *   <li>老师只能批改、删除自己课上的作业；挂在作业任务下的提交按课程空间同一套规则批改（分数范围、改为已批改）；</li>
 *   <li>按 ID 查看也做归属检查，看不到别人的作业。</li>
 * </ul>
 */
@Service
public class HomeworkService extends CrudService<Homework> {

    private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private HomeworkMapper homeworkMapper;
    @Resource
    private CourseMapper courseMapper;
    @Resource
    private ChoiceMapper choiceMapper;
    @Resource
    private AssignmentService assignmentService;
    @Resource
    private UploadedFiles uploadedFiles;

    @Override
    protected CrudMapper<Homework> getMapper() {
        return homeworkMapper;
    }

    /**
     * 新增：只登记「直接上传」的作业，归属到课程的任课教师
     */
    @Override
    public void add(Homework homework) {
        Course course = homework.getCourseId() == null ? null : courseMapper.selectById(homework.getCourseId());
        if (course == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Account current = TokenUtils.getCurrentUser();
        if (isStudent(current)) {
            if (choiceMapper.countByStudentAndCourse(current.getId(), course.getId()) == 0) {
                throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
            }
            homework.setStudentId(current.getId());
            homework.setScore(null);
            homework.setDescr(null);
        }
        homework.setId(null);
        homework.setAssignmentId(null);
        homework.setStatus(null);
        homework.setFile(uploadedFiles.require(blankToNull(homework.getFile())));
        homework.setSubmitTime(LocalDateTime.now(AppTime.clock()).format(MINUTES));
        homework.setTeacherId(course.getTeacherId());
        homeworkMapper.insert(homework);
    }

    /**
     * 修改：学生改自己没批改的直接上传作业（只改内容和附件）；老师和管理员批改
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(Homework homework) {
        Homework db = homework.getId() == null ? null : homeworkMapper.selectById(homework.getId());
        if (db == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Account current = TokenUtils.getCurrentUser();
        if (isStudent(current)) {
            requireOwnLooseUngraded(current, db);
            Homework patch = new Homework();
            patch.setId(db.getId());
            patch.setContent(homework.getContent());
            // 旧版表单会把整行连同原附件地址一起提交：没换附件就不动它（老数据里可能是早年的完整地址，过不了校验）
            String file = blankToNull(homework.getFile());
            if (file != null && !file.equals(db.getFile())) {
                patch.setFile(uploadedFiles.require(file));
                patch.setFileName(homework.getFileName());
            }
            homeworkMapper.updateById(patch);
            return;
        }
        requireTeachingOf(current, db);
        if (db.getAssignmentId() != null) {
            assignmentService.grade(db.getCourseId(), db.getId(), parseScore(homework.getScore()), homework.getDescr());
            return;
        }
        Homework patch = new Homework();
        patch.setId(db.getId());
        patch.setScore(homework.getScore());
        patch.setDescr(homework.getDescr());
        homeworkMapper.updateById(patch);
    }

    /**
     * 删除：学生只能删自己没批改的直接上传作业；老师只能删自己课上的
     */
    @Override
    public void deleteById(Integer id) {
        Homework db = id == null ? null : homeworkMapper.selectById(id);
        if (db == null) {
            return;
        }
        Account current = TokenUtils.getCurrentUser();
        if (isStudent(current)) {
            requireOwnLooseUngraded(current, db);
        } else {
            requireTeachingOf(current, db);
        }
        homeworkMapper.deleteById(id);
    }

    /** 批量删除逐条走 {@link #deleteById}，每一条都做归属检查 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    /** 按 ID 查看：学生只能看自己的，老师只能看自己课上的；看不到的当作不存在 */
    @Override
    public Homework selectById(Integer id) {
        Homework db = homeworkMapper.selectById(id);
        if (db == null) {
            return null;
        }
        Account current = TokenUtils.getCurrentUser();
        if (isStudent(current) && !current.getId().equals(db.getStudentId())) {
            return null;
        }
        if (isTeacher(current) && !current.getId().equals(db.getTeacherId())) {
            return null;
        }
        return db;
    }

    /**
     * 数据行级隔离：学生/教师只能查看自己的作业（分页与全量接口统一生效）
     */
    @Override
    protected void applyDataScope(Homework homework) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (isStudent(currentUser)) {
            homework.setStudentId(currentUser.getId());
        }
        if (isTeacher(currentUser)) {
            homework.setTeacherId(currentUser.getId());
        }
    }

    private static void requireOwnLooseUngraded(Account current, Homework db) {
        boolean own = current.getId() != null && current.getId().equals(db.getStudentId());
        // 挂在作业任务下的提交要在课程空间里交、改（那边管截止时间）；批改过的不能再动
        if (!own || db.getAssignmentId() != null || db.getScore() != null) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
    }

    private static void requireTeachingOf(Account current, Homework db) {
        if (isTeacher(current) && !current.getId().equals(db.getTeacherId())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        if (!isTeacher(current) && !RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
    }

    private static boolean isStudent(Account account) {
        return RoleEnum.STUDENT.name().equals(account.getRole());
    }

    private static boolean isTeacher(Account account) {
        return RoleEnum.TEACHER.name().equals(account.getRole());
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static Double parseScore(String score) {
        if (score == null || score.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(score.trim());
        } catch (NumberFormatException e) {
            throw new CustomException(ResultCodeEnum.HOMEWORK_SCORE_ERROR);
        }
    }
}
