package com.example.mapper;

import com.example.entity.Score;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 操作 score 相关数据接口（通用增删改查见 {@link CrudMapper}）
 */
public interface ScoreMapper extends CrudMapper<Score> {

    @Select("select * from score where course_id = #{courseId} and student_id = #{studentId}")
    Score selectByCourceIdAndStudentId(@Param("courseId") Integer courseId, @Param("studentId") Integer studentId);

    /**
     * 成绩分段统计：返回 bucket（A~E 档位码）与 value（人次）。
     * 分桶交给数据库做，不再把全量成绩捞进内存后 stream filter 五遍。
     * 过滤条件由实体承载（教师/学生的数据隔离范围）。
     */
    List<Map<String, Object>> selectScoreDistribution(Score score);

    /** 成绩册：一门课全部学生的成绩行（含草稿） */
    List<Score> selectByCourse(Integer courseId);

    /** 成绩册新增一行（各项成绩可空），回填 id */
    int insertGradebookRow(Score score);

    /** 成绩册保存一行：空值照样写入（与 updateById 只改非空字段不同） */
    int updateGradebookRow(Score score);

    /** 成绩单上的课：学生选着的课，加上有成绩记录的课（选课记录没了、成绩还在的也要列出来），带任课教师 */
    @Select("select c.id as courseId, c.name as courseName, c.type as type, c.score as credit, c.status as courseStatus,"
            + " t.name as teacherName from course c left join teacher t on c.teacher_id = t.id"
            + " where c.id in (select course_id from choice where student_id = #{studentId}"
            + " union select course_id from score where student_id = #{studentId})")
    List<Map<String, Object>> selectTranscriptCourses(Integer studentId);
}
