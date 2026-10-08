package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 课程评价：五个维度各 1~5 分，外加一段文字。教师看到的是匿名汇总，看不到是谁评的。
 */
@Data
public class CourseEval implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer courseId;
    private Integer teacherId;
    private Integer studentId;
    /** 教学态度 */
    private Integer attitude;
    /** 教学内容 */
    private Integer contentScore;
    /** 教学方法 */
    private Integer method;
    /** 教学效果 */
    private Integer effect;
    /** 课后辅导 */
    private Integer support;
    private String comment;
    /** yyyy-MM-dd HH:mm */
    private String createTime;
}
