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
}
