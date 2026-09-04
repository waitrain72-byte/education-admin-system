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

    private String courseName;
    private String studentName;
    private String teacherName;

}