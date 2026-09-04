package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Attendance;
import com.example.exception.CustomException;
import com.example.mapper.AttendanceMapper;
import com.example.mapper.CrudMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 考勤信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class AttendanceService extends CrudService<Attendance> {

    @Resource
    private AttendanceMapper attendanceMapper;

    @Override
    protected CrudMapper<Attendance> getMapper() {
        return attendanceMapper;
    }

    /**
     * 新增：判断同一个学生同一门课同一天的考勤记录只能是一条
     */
    @Override
    public void add(Attendance attendance) {
        Attendance dbAttendance = attendanceMapper.selectByStudentIdAndCourseIdAndTime(attendance.getStudentId(), attendance.getCourseId(), attendance.getTime());
        if (ObjectUtil.isNotEmpty(dbAttendance)) {
            throw new CustomException(ResultCodeEnum.ATTENDANCE_ALREADY_ERROR);
        }
        attendanceMapper.insert(attendance);
    }

    /**
     * 数据行级隔离：教师/学生只能查看自己的考勤（分页与全量接口统一生效）
     */
    @Override
    protected void applyDataScope(Attendance attendance) {
        Account currentUser = TokenUtils.getCurrentUser();
        if (RoleEnum.TEACHER.name().equals(currentUser.getRole())) {
            attendance.setTeacherId(currentUser.getId());
        }
        if (RoleEnum.STUDENT.name().equals(currentUser.getRole())) {
            attendance.setStudentId(currentUser.getId());
        }
    }
}
