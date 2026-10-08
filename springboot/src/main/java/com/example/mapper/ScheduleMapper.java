package com.example.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 日程页的聚合查询：只读。
 */
public interface ScheduleMapper {

    /** 给定课程在 [from, to) 内截止的作业：id / courseId / courseName / title / deadline（时间均为 yyyy-MM-dd HH:mm） */
    @Select("<script>select a.id, a.course_id as courseId, c.name as courseName, a.title, a.deadline from assignment a"
            + " join course c on c.id = a.course_id"
            + " where a.deadline &gt;= #{from} and a.deadline &lt; #{to}"
            + " and a.course_id in <foreach collection='courseIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + " order by a.deadline</script>")
    List<Map<String, Object>> assignmentsBetween(@Param("courseIds") List<Integer> courseIds,
                                                 @Param("from") String from, @Param("to") String to);

    /** 学生交过的作业任务 ID（日程里把已交的作业标成完成） */
    @Select("select assignment_id from homework where student_id = #{studentId} and assignment_id is not null")
    List<Integer> submittedAssignmentIds(Integer studentId);
}
