package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 选课信息表
 */
@Data
public class Choice implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    /** 授课教师 */
    private Integer teacherId;
    /** 学生ID */
    private Integer studentId;
    /** 课程ID */
    private Integer courseId;


    /** 课程名称 */
    private String name;
    /** 课程类型 */
    private String type;
    /** 课程学分 */
    private Integer score;
    /** 上课人数 */
    private Integer num;
    /** 上课教室 */
    private String room;
    /** 周几 */
    private String week;
    /** 第几大节 */
    private String segment;
    /** 上课状态 */
    private String status;


    private String teacherName;
    private String studentName;

}