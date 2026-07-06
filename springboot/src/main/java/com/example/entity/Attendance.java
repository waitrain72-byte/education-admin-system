package com.example.entity;

import lombok.Data;

import java.io.Serializable;


/**
 * 考勤信息表
 */
@Data
public class Attendance implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    private Integer studentId;
    private Integer courseId;
    private Integer teacherId;
    private String time;
    private String status;

    private String studentName;
    private String courseName;
    private String teacherName;

}