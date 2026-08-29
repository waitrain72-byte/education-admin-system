package com.example.mapper;

import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 数据大屏聚合统计 Mapper（只读查询，全部 #{} 安全）
 */
public interface DashboardMapper {

    @Select("SELECT COUNT(*) FROM student")
    int countStudent();

    @Select("SELECT COUNT(*) FROM teacher")
    int countTeacher();

    @Select("SELECT COUNT(*) FROM course")
    int countCourse();

    @Select("SELECT COUNT(*) FROM choice")
    int countChoice();

    @Select("SELECT COUNT(*) FROM sys_login_log WHERE DATE(create_time) = CURDATE()")
    int countLoginToday();

    @Select("SELECT COUNT(*) FROM sys_login_log WHERE DATE(create_time) >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)")
    int countLoginWeek();

    @Select("SELECT COUNT(*) FROM apply WHERE status = '待审核'")
    int countPendingApply();

    @Select("SELECT COUNT(*) FROM homework WHERE score IS NULL")
    int countUngradedHomework();

    @Select("SELECT c.name AS name, COUNT(s.id) AS value FROM college c " +
            "LEFT JOIN student s ON s.college_id = c.id " +
            "GROUP BY c.id, c.name ORDER BY value DESC")
    List<Map<String, Object>> collegeDist();

    @Select("SELECT co.name AS name, COUNT(*) AS value FROM choice ch " +
            "JOIN course co ON ch.course_id = co.id " +
            "GROUP BY ch.course_id, co.name ORDER BY value DESC LIMIT 5")
    List<Map<String, Object>> courseTop();

    @Select("SELECT title AS name, COUNT(*) AS value FROM teacher WHERE title IS NOT NULL GROUP BY title")
    List<Map<String, Object>> titleDist();

    @Select("SELECT DATE(create_time) AS date, COUNT(*) AS value FROM sys_login_log " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE(create_time)")
    List<Map<String, Object>> loginTrend();
}
