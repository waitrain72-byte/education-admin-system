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
    /** 考勤分（成绩册：按考勤记录自动折算，老师可改） */
    private Double attendanceScore;
    /** 作业分（成绩册：按已批改作业自动折算，老师可改） */
    private Double homeworkScore;
    /** 草稿 / 已发布：学生只看得到已发布的成绩，学分也只按已发布且及格的累计 */
    private String status;
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