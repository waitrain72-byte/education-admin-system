package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.exception.CustomException;
import com.example.mapper.CourseMapper;
import com.example.mapper.CrudMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 课程信息表业务处理（通用增删改查见 {@link CrudService}）
 *
 * <p>角色策略：新增/删除课程仅管理员；教师只能修改「自己开设课程」的
 * 上课教室/周几/大节/上课状态（排课调整），课程名称/类型/学分/人数等不可改。</p>
 */
@Service
public class CourseService extends CrudService<Course> {

    @Resource
    private CourseMapper courseMapper;

    @Override
    protected CrudMapper<Course> getMapper() {
        return courseMapper;
    }

    /**
     * 数据行级隔离：教师只能查看自己开设的课程（分页与全量接口统一生效）
     */
    @Override
    protected void applyDataScope(Course course) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
            course.setTeacherId(currentUser.getId());
        }
    }

    /**
     * 新增课程：仅管理员（教师只做排课调整）
     */
    @Override
    public void add(Course course) {
        requireAdmin();
        checkRoomOccupied(course);
        super.add(course);
    }

    /**
     * 删除课程：仅管理员
     */
    @Override
    public void deleteById(Integer id) {
        requireAdmin();
        super.deleteById(id);
    }

    /**
     * 批量删除课程：仅管理员
     */
    @Override
    public void deleteBatch(List<Integer> ids) {
        requireAdmin();
        super.deleteBatch(ids);
    }

    /**
     * 修改课程：管理员可改全部字段；教师仅能改「自己开设课程」的
     * 教室/周几/大节/上课状态（其余字段强制保留原值），并重新校验教室占用。
     */
    @Override
    public void updateById(Course course) {
        if (course == null || course.getId() == null) {
            // 无 id 的更新无法排除自身，会误报教室占用/更新 0 行，直接按参数缺失拦截
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
            Course existing = courseMapper.selectById(course.getId());
            if (existing == null) {
                throw new CustomException(ResultCodeEnum.PARAM_ERROR);
            }
            if (!currentUser.getId().equals(existing.getTeacherId())) {
                throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
            }
            // 字段白名单：教师仅可调整排课四项，其余强制保留原值
            Course merged = new Course();
            merged.setId(existing.getId());
            merged.setRoom(course.getRoom() != null ? course.getRoom() : existing.getRoom());
            merged.setWeek(course.getWeek() != null ? course.getWeek() : existing.getWeek());
            merged.setSegment(course.getSegment() != null ? course.getSegment() : existing.getSegment());
            merged.setStatus(course.getStatus() != null ? course.getStatus() : existing.getStatus());
            checkRoomOccupied(merged);
            courseMapper.updateById(merged);
            return;
        }
        checkRoomOccupied(course);
        super.updateById(course);
    }

    /**
     * 教室占用查询：返回同一「教室 + 星期 + 大节」已存在的课程（供表单在分配教室前实时提示），无占用返回 null。
     * 直接走 Mapper、不经 applyDataScope——教室是否被占用与登录角色无关（他人开设的课同样占用教室）。
     */
    public Course selectRoomOccupied(String room, String week, String segment, Integer excludeId) {
        Course probe = new Course();
        probe.setRoom(room);
        probe.setWeek(week);
        probe.setSegment(segment);
        probe.setId(excludeId);
        return courseMapper.selectRoomOccupied(probe);
    }

    /**
     * 教室占用校验：同一「教室 + 星期 + 大节」不允许两门课重叠；
     * room/week/segment 任一为空（未排课）时跳过；更新时排除自身 id。
     */
    private void checkRoomOccupied(Course course) {
        if (course == null || isBlank(course.getRoom()) || isBlank(course.getWeek()) || isBlank(course.getSegment())) {
            return;
        }
        Course probe = new Course();
        probe.setRoom(course.getRoom());
        probe.setWeek(course.getWeek());
        probe.setSegment(course.getSegment());
        probe.setId(course.getId());
        Course occupied = courseMapper.selectRoomOccupied(probe);
        if (occupied != null) {
            throw new CustomException(
                ResultCodeEnum.ROOM_OCCUPIED_ERROR.code,
                "该教室在此时间段已被《" + occupied.getName() + "》占用，请更换教室或上课时间"
            );
        }
    }

    /**
     * 仅管理员可执行（教师角色按业务策略不允许增删课程）
     */
    private void requireAdmin() {
        Account currentUser = TokenUtils.getCurrentUser();
        if (!RoleEnum.ADMIN.name().equals(currentUser.getRole())) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
