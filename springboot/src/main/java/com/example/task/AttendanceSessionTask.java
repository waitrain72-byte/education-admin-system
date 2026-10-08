package com.example.task;

import com.example.entity.AttendanceSession;
import com.example.service.AttendanceSessionService;
import com.example.service.CourseSpaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 定时任务：课堂签到收尾。
 * 每 20 秒把已过截止时间、还标着进行中的签到结束掉，没签到的学生记缺勤（有请假的记请假），
 * 再通知正在看课程空间的页面刷新。老师手动结束的不经过这里。
 */
@Component
public class AttendanceSessionTask {

    private static final Logger log = LoggerFactory.getLogger(AttendanceSessionTask.class);

    @Resource
    private AttendanceSessionService attendanceSessionService;
    @Resource
    private CourseSpaceService courseSpaceService;

    @Scheduled(fixedDelay = 20000, initialDelay = 20000)
    public void finishExpiredSessions() {
        try {
            for (AttendanceSession session : attendanceSessionService.expiredSessions()) {
                int marked = attendanceSessionService.finishExpired(session);
                courseSpaceService.emit(session.getCourseId(), "attendance");
                log.info("签到 #{} 已到截止时间，自动结束，补记考勤 {} 人", session.getId(), marked);
            }
        } catch (Exception e) {
            log.warn("签到收尾失败：{}", e.getMessage());
        }
    }
}
