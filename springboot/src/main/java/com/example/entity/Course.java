package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 课程信息表
*/
@Data
public class Course implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    /** 课程名称 */
    private String name;
    /** 课程类型 */
    private String type;
    /** 授课教师 */
    private Integer teacherId;
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

    /** 总评权重（%）：考勤、作业、平时、期末，四项合计 100；默认 0/0/30/70 与改版前的计算方式一致 */
    private Integer weightAttendance;
    private Integer weightHomework;
    private Integer weightOrdinary;
    private Integer weightExam;
    /** 课程简介（课程空间概览、课程广场卡片展示） */
    private String intro;

    private String teacherName;

}