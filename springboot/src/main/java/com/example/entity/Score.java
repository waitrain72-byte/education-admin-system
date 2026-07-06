package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 *成绩信息表
 */
@Data
public class Score implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    /** 学生ID */
    private Integer studentId;
    /** 课程ID */
    private Integer courseId;
    /** 教师ID */
    private Integer teacherId;
    /** 平时分 */
    private Double ordinaryScore;
    /** 考试分 */
    private Double examScore;
    /** 总成绩 */
    private Double score;

    private String studentName;
    private String courseName;
    private String teacherName;

}