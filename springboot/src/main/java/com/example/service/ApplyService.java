package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.AppTime;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Apply;
import com.example.entity.Attendance;
import com.example.exception.CustomException;
import com.example.mapper.ApplyMapper;
import com.example.mapper.AttendanceMapper;
import com.example.mapper.CrudMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 请假信息表业务处理（通用增删改查见 {@link CrudService}）。
 *
 * <ul>
 *   <li>学生：只能以自己的名义提交（状态一律「待审核」），只能改、撤回自己还没审核的申请；</li>
 *   <li>管理员审核：改成「审核通过 / 审核不通过」并写审核意见；批准时把请假期间已经过去的日子里，
 *       这名学生要上的课的考勤记为「请假」（没有记录的补一条，记了缺勤的改成请假，到了课的不动）；
 *       还没到的日子由签到结束时按已批请假记请假（见 {@link AttendanceSessionService}）；</li>
 *   <li>日期：开始日最早可以是 7 天前（补请假），一次最多连续 30 天。</li>
 * </ul>
 */
@Service
public class ApplyService extends CrudService<Apply> {

    public static final String PENDING = "待审核";
    public static final String APPROVED = "审核通过";
    public static final String REJECTED = "审核不通过";
    static final int MAX_DAYS = 30;
    static final int MAX_BACKDATE_DAYS = 7;
    static final int MAX_TEXT = 500;

    @Resource
    private ApplyMapper applyMapper;
    @Resource
    private AttendanceMapper attendanceMapper;
    @Resource
    private ScheduleService scheduleService;

    @Override
    protected CrudMapper<Apply> getMapper() {
        return applyMapper;
    }

    /** 提交请假：学生只能给自己请；状态一律待审核 */
    @Override
    public void add(Apply apply) {
        Account current = TokenUtils.getCurrentUser();
        if (isStudent(current)) {
            apply.setStudentId(current.getId());
        } else if (!isAdmin(current) || apply.getStudentId() == null) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        validate(apply);
        apply.setId(null);
        apply.setStatus(PENDING);
        apply.setDescr(null);
        applyMapper.insert(apply);
    }

    /**
     * 修改：学生改自己还没审核的申请（只改理由和日期）；管理员审核。
     *
     * @return 是否完成了一次审核（控制器据此通知学生）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Apply apply) {
        Apply db = apply.getId() == null ? null : applyMapper.selectById(apply.getId());
        if (db == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Account current = TokenUtils.getCurrentUser();
        if (isStudent(current)) {
            requireOwnPending(current, db);
            Apply patch = new Apply();
            patch.setId(db.getId());
            patch.setContent(apply.getContent() != null ? apply.getContent() : db.getContent());
            patch.setTime(apply.getTime() != null ? apply.getTime() : db.getTime());
            patch.setDay(apply.getDay() != null ? apply.getDay() : db.getDay());
            validate(patch);
            applyMapper.updateById(patch);
            return false;
        }
        if (!isAdmin(current)) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        String status = apply.getStatus();
        if (status != null && !Arrays.asList(PENDING, APPROVED, REJECTED).contains(status)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Apply patch = new Apply();
        patch.setId(db.getId());
        patch.setStatus(status);
        patch.setDescr(apply.getDescr());
        applyMapper.updateById(patch);
        boolean reviewed = status != null && !status.equals(db.getStatus()) && !PENDING.equals(status);
        if (APPROVED.equals(status) && !APPROVED.equals(db.getStatus())) {
            markLeave(db);
        }
        return reviewed;
    }

    /** 兼容通用接口：走 {@link #update} 的规则 */
    @Override
    public void updateById(Apply apply) {
        update(apply);
    }

    /** 撤回：学生只能撤自己还没审核的；管理员可删任意一条 */
    @Override
    public void deleteById(Integer id) {
        Apply db = id == null ? null : applyMapper.selectById(id);
        if (db == null) {
            return;
        }
        Account current = TokenUtils.getCurrentUser();
        if (isStudent(current)) {
            requireOwnPending(current, db);
        } else if (!isAdmin(current)) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        applyMapper.deleteById(id);
    }

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

    /** 学生按 ID 只看得到自己的申请 */
    @Override
    public Apply selectById(Integer id) {
        Apply db = applyMapper.selectById(id);
        Account current = TokenUtils.getCurrentUser();
        if (db != null && isStudent(current) && !current.getId().equals(db.getStudentId())) {
            return null;
        }
        return db;
    }

    /** 请假预览：从 from 起连续 days 天里，当前学生要上的课 */
    public List<Map<String, Object>> preview(String from, Integer days) {
        Account current = TokenUtils.getCurrentUser();
        if (!isStudent(current)) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        LocalDate start = parseDate(from);
        int n = days == null ? 1 : days;
        requireRange(start, n);
        return scheduleService.classesBetween(current.getId(), start, start.plusDays(n - 1L));
    }

    /**
     * 批准后补记考勤：请假期间已经过去（含今天）的日子里，这名学生要上的课没有考勤的补「请假」，
     * 记了缺勤的改成「请假」；到了课（正常、迟到、早退）的不动。
     */
    void markLeave(Apply apply) {
        LocalDate start = parseDateOrNull(apply.getTime());
        if (start == null || apply.getStudentId() == null) {
            return;
        }
        int days = apply.getDay() == null || apply.getDay() < 1 ? 1 : apply.getDay();
        LocalDate today = LocalDate.now(AppTime.clock());
        LocalDate end = start.plusDays(days - 1L);
        if (end.isAfter(today)) {
            end = today;
        }
        if (end.isBefore(start)) {
            return;
        }
        for (Map<String, Object> cls : scheduleService.classesBetween(apply.getStudentId(), start, end)) {
            Integer courseId = (Integer) cls.get("courseId");
            String date = String.valueOf(cls.get("date"));
            Attendance existing = attendanceMapper.selectByStudentIdAndCourseIdAndTime(apply.getStudentId(), courseId, date);
            if (existing == null) {
                Attendance record = new Attendance();
                record.setStudentId(apply.getStudentId());
                record.setCourseId(courseId);
                record.setTeacherId((Integer) cls.get("teacherId"));
                record.setTime(date);
                record.setStatus(AttendanceSessionService.LEAVE);
                attendanceMapper.insert(record);
            } else if (AttendanceSessionService.ABSENT.equals(existing.getStatus())) {
                existing.setStatus(AttendanceSessionService.LEAVE);
                attendanceMapper.updateById(existing);
            }
        }
    }

    /**
     * 数据行级隔离：学生只能看自己的请假记录（分页与全量接口统一生效）
     */
    @Override
    protected void applyDataScope(Apply apply) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (isStudent(currentUser)) {
            apply.setStudentId(currentUser.getId());
        }
    }

    private void validate(Apply apply) {
        String content = StrUtil.trimToNull(apply.getContent());
        if (content == null || apply.getTime() == null || apply.getDay() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (content.length() > MAX_TEXT) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        apply.setContent(content);
        LocalDate start = parseDate(apply.getTime());
        requireRange(start, apply.getDay());
        apply.setTime(start.toString());
    }

    /** 开始日最早 7 天前，天数 1~30 */
    private static void requireRange(LocalDate start, int days) {
        LocalDate today = LocalDate.now(AppTime.clock());
        if (days < 1 || days > MAX_DAYS || start.isBefore(today.minusDays(MAX_BACKDATE_DAYS))) {
            throw new CustomException(ResultCodeEnum.LEAVE_RANGE_ERROR);
        }
    }

    private static void requireOwnPending(Account current, Apply db) {
        if (!current.getId().equals(db.getStudentId())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        if (!PENDING.equals(db.getStatus())) {
            throw new CustomException(ResultCodeEnum.LEAVE_LOCKED_ERROR);
        }
    }

    private static LocalDate parseDate(String text) {
        LocalDate date = parseDateOrNull(text);
        if (date == null) {
            throw new CustomException(ResultCodeEnum.LEAVE_RANGE_ERROR);
        }
        return date;
    }

    private static LocalDate parseDateOrNull(String text) {
        if (text == null || text.trim().length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim().substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static boolean isStudent(Account account) {
        return RoleEnum.STUDENT.name().equals(account.getRole());
    }

    private static boolean isAdmin(Account account) {
        return RoleEnum.ADMIN.name().equals(account.getRole());
    }
}
