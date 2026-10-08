package com.example.entity;

import lombok.Data;

import java.io.Serializable;


/**
 * 作业信息
 */
@Data
public class Homework implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    private String content;
    private Integer courseId;
    private Integer studentId;
    private Integer teacherId;
    private String file;
    private String score;
    private String descr;
    /** 所属作业任务（旧版自由提交为空） */
    private Integer assignmentId;
    /** 提交时间 yyyy-MM-dd HH:mm */
    private String submitTime;
    /** 已提交 / 已批改 */
    private String status;
    /** 附件原文件名（file 存的是去重后的存储名，展示与下载时用它） */
    private String fileName;

    private String courseName;
    private String studentName;
    private String teacherName;

}