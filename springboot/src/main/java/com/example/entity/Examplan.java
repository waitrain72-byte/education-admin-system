package com.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

/**
 * 考试安排表
*/
@Data
public class Examplan implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Integer id;
    /** 标题 */
    private String name;
    /** 内容 */
    private String content;
    /** 发布时间（新增时由后端生成） */
    private String time;
    /** 考试时间（yyyy-MM-dd HH:mm），前端据此显示考试倒计时；历史数据可能为空 */
    private String examTime;
    /** 所属课程：空 = 全校考试（人人可见），非空 = 只给这门课的任课教师和选课学生看 */
    private Integer courseId;

    /** 所属课程名（查询时关联 course 表得到，不是表字段） */
    private String courseName;

    /**
     * 可见范围（查询条件，不是表字段）：非空时只查全校考试 + 这名学生选了的课 / 这位老师教的课的考试。
     * 由 ExamplanService 按登录角色填写，前端传来的值会被覆盖，也不出现在接口返回里。
     */
    @JsonIgnore
    private Integer scopeStudentId;
    @JsonIgnore
    private Integer scopeTeacherId;
}