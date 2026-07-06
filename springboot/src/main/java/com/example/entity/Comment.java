package com.example.entity;

import lombok.Data;

import java.io.Serializable;
/**
 * 评教
 */
@Data
public class Comment implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    private String name;
    private String teacher;
    private String student;
    private String content;
    private String time;

}