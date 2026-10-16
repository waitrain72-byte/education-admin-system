package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.AppTime;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Examplan;
import com.example.exception.CustomException;
import com.example.mapper.CourseMapper;
import com.example.mapper.CrudMapper;
import com.example.mapper.ExamplanMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * 考试安排表业务处理（通用增删改查见 {@link CrudService}）
 *
 * <p>考试分两种：不填所属课程的是全校考试，人人可见；填了课程的只给这门课的任课教师和选课学生看。
 * 列表查询按登录角色收窄（{@link #applyDataScope}），首页、日程、数据大屏用 {@link ExamplanMapper#selectBetween} 同一套范围。</p>
 */
@Service
public class ExamplanService extends CrudService<Examplan> {

    /** 考试时间格式（与 Web / 小程序的日期时间选择器一致）；STRICT 拒绝 2 月 30 日、24:00 这类不存在的时间 */
    private static final DateTimeFormatter EXAM_TIME_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);

    @Resource
    private ExamplanMapper examplanMapper;
    @Resource
    private CourseMapper courseMapper;

    @Override
    protected CrudMapper<Examplan> getMapper() {
        return examplanMapper;
    }

    /**
     * 新增：校验考试时间与所属课程，生成发布时间
     */
    @Override
    public void add(Examplan examplan) {
        checkExamTime(examplan.getExamTime());
        checkCourse(examplan.getCourseId());
        examplan.setTime(AppTime.now());
        examplanMapper.insert(examplan);
    }

    /**
     * 修改：校验考试时间与所属课程；发布时间只在新增时生成，修改时不允许改写。
     * 所属课程按表单原样写回，空 = 改成全校考试。
     */
    @Override
    public void updateById(Examplan examplan) {
        checkExamTime(examplan.getExamTime());
        checkCourse(examplan.getCourseId());
        examplan.setTime(null);
        super.updateById(examplan);
    }

    /**
     * 列表按角色收窄：管理员看全部；教师只多看到自己教的课的考试，学生只多看到自己选的课的考试。
     * 前端传来的范围参数一律以登录身份为准覆盖。
     */
    @Override
    protected void applyDataScope(Examplan examplan) {
        Account current = TokenUtils.getCurrentUser();
        examplan.setScopeStudentId(null);
        examplan.setScopeTeacherId(null);
        if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            return;
        }
        if (current.getId() == null) {
            throw new CustomException(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        if (RoleEnum.TEACHER.name().equals(current.getRole())) {
            examplan.setScopeTeacherId(current.getId());
        } else {
            examplan.setScopeStudentId(current.getId());
        }
    }

    /**
     * 按 ID 查也受同一可见范围约束：全校考试人人可查，课程考试只有这门课的师生（和管理员）查得到，否则返回空
     */
    @Override
    public Examplan selectById(Integer id) {
        Examplan db = examplanMapper.selectById(id);
        if (db == null || db.getCourseId() == null) {
            return db;
        }
        Examplan probe = new Examplan();
        probe.setId(id);
        applyDataScope(probe);
        if (probe.getScopeStudentId() == null && probe.getScopeTeacherId() == null) {
            return db;
        }
        return examplanMapper.selectAll(probe).isEmpty() ? null : db;
    }

    /**
     * 考试时间是否合法：为空时放行（历史数据没有考试时间，前端不显示倒计时）；
     * 填写了就必须是真实存在的 yyyy-MM-dd HH:mm。
     */
    static boolean isValidExamTime(String examTime) {
        if (StrUtil.isBlank(examTime)) {
            return true;
        }
        try {
            LocalDateTime.parse(examTime, EXAM_TIME_FORMAT);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private void checkExamTime(String examTime) {
        if (!isValidExamTime(examTime)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
    }

    /** 填了所属课程，这门课就得存在 */
    private void checkCourse(Integer courseId) {
        if (courseId != null && courseMapper.selectById(courseId) == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
    }
}
