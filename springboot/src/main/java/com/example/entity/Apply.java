package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 请假信息
 */
@Data
public class Apply implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    private Integer studentId;
    private String content;
    private String time;
    private Integer day;
    private String status;
    private String descr;
    
    private String studentName;

}