package com.example.mapper;

import com.example.entity.Attendance;
import com.example.entity.Homework;
import com.example.entity.Student;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 课程空间里按「一门课」聚合的查询：花名册、考勤与作业的按人统计。只读查询 + 删除作业任务下的提交。
 */
public interface CourseSpaceMapper {

    /** 选了这门课的学生（花名册），只取展示需要的列 */
    @Select("select s.id, s.username, s.name, s.avatar, cl.name as className from choice ch"
            + " join student s on ch.student_id = s.id"
            + " left join classes cl on s.class_id = cl.id"
            + " where ch.course_id = #{courseId} order by s.id")
    List<Student> selectMembers(Integer courseId);

    /** 选了这门课的学生 ID */
    @Select("select student_id from choice where course_id = #{courseId}")
    List<Integer> selectMemberIds(Integer courseId);

    /** 这门课每个学生每种考勤状态的次数：studentId / status / total */
    @Select("select student_id as studentId, status, count(*) as total from attendance"
            + " where course_id = #{courseId} and status is not null group by student_id, status")
    List<Map<String, Object>> attendanceCountsByStudent(Integer courseId);

    /** 这门课按上课日期汇总的考勤：date / status / total（最近的日期在前） */
    @Select("select time as date, status, count(*) as total from attendance"
            + " where course_id = #{courseId} and status is not null group by time, status order by time desc")
    List<Map<String, Object>> attendanceCountsByDate(Integer courseId);

    /** 某个学生在这门课的考勤记录（最近的在前） */
    @Select("select id, time, status, session_id from attendance"
            + " where course_id = #{courseId} and student_id = #{studentId} order by time desc")
    List<Attendance> selectAttendanceOfStudent(@Param("courseId") Integer courseId, @Param("studentId") Integer studentId);

    /** 某一天这门课的考勤记录（签到面板的名单状态） */
    @Select("select id, student_id, time, status, session_id from attendance where course_id = #{courseId} and time = #{date}")
    List<Attendance> selectAttendanceOfDate(@Param("courseId") Integer courseId, @Param("date") String date);

    /** 这门课全部「按作业任务」提交的记录 */
    @Select("select * from homework where course_id = #{courseId} and assignment_id is not null")
    List<Homework> selectSubmissionsOfCourse(Integer courseId);

    /** 某个作业任务下的全部提交 */
    @Select("select * from homework where assignment_id = #{assignmentId}")
    List<Homework> selectSubmissionsOfAssignment(Integer assignmentId);

    @Select("select * from homework where assignment_id = #{assignmentId} and student_id = #{studentId} limit 1")
    Homework selectSubmission(@Param("assignmentId") Integer assignmentId, @Param("studentId") Integer studentId);

    @Delete("delete from homework where assignment_id = #{assignmentId}")
    int deleteSubmissionsOfAssignment(Integer assignmentId);

    /** 旧版「学生直接上传」的作业（没有挂在作业任务下），课程作业页单独列出 */
    @Select("select h.*, s.name as studentName from homework h left join student s on h.student_id = s.id"
            + " where h.course_id = #{courseId} and h.assignment_id is null order by h.id desc")
    List<Homework> selectLooseSubmissions(Integer courseId);
}
