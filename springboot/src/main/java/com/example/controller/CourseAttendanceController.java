package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.NoRepeatSubmit;
import com.example.common.annotation.RequirePermission;
import com.example.entity.Attendance;
import com.example.service.AttendanceSessionService;
import com.example.service.CourseSpaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 课程空间 · 课堂签到。权限沿用考勤模块：查看 attendance:view，发起 / 结束 / 登记 attendance:manage，
 * 学生签到 attendance:checkin；能不能操作这门课再由 Service 按与课程的关系判断。
 */
@RestController
@RequestMapping("/course/{courseId}/attendance")
@RequirePermission(module = "attendance")
public class CourseAttendanceController {

    @Resource
    private AttendanceSessionService attendanceSessionService;
    @Resource
    private CourseSpaceService courseSpaceService;

    @GetMapping
    public Result panel(@PathVariable Integer courseId, @RequestParam(required = false) String date) {
        return Result.success(attendanceSessionService.panel(courseId, date));
    }

    /** 发起签到，body 可带 minutes（1~30，默认 5） */
    @PostMapping("/sessions")
    public Result start(@PathVariable Integer courseId, @RequestBody(required = false) Map<String, Object> body) {
        Object minutes = body == null ? null : body.get("minutes");
        Map<String, Object> session = attendanceSessionService.start(courseId,
                minutes instanceof Number ? ((Number) minutes).intValue() : null);
        courseSpaceService.emit(courseId, "attendance");
        return Result.success(session);
    }

    @PostMapping("/sessions/{sessionId}/finish")
    public Result finish(@PathVariable Integer courseId, @PathVariable Integer sessionId) {
        Map<String, Object> result = attendanceSessionService.finish(courseId, sessionId);
        courseSpaceService.emit(courseId, "attendance");
        return Result.success(result);
    }

    /** 学生签到：body 为 {code} */
    @NoRepeatSubmit(interval = 1000)
    @RequirePermission("attendance:checkin")
    @PostMapping("/checkin")
    public Result checkin(@PathVariable Integer courseId, @RequestBody Map<String, Object> body) {
        Object code = body == null ? null : body.get("code");
        Attendance record = attendanceSessionService.checkin(courseId, code == null ? null : String.valueOf(code));
        courseSpaceService.emitToTeacher(courseId, "attendance");
        return Result.success(record);
    }

    /** 老师登记或修改某个学生某天的考勤：body 为 {studentId, time(yyyy-MM-dd), status} */
    @PutMapping("/records")
    public Result mark(@PathVariable Integer courseId, @RequestBody Attendance body) {
        return Result.success(attendanceSessionService.mark(courseId,
                body.getStudentId(), body.getTime(), body.getStatus()));
    }
}
