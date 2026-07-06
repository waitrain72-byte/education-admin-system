package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 学生
*/
@Data
public class Student extends Account implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    private String username;
    private String password;
    private String name;
    private String avatar;
    private String role;
    private Integer collegeId;
    private Integer specialityId;
    private Integer classId;
    private Integer score;

    /*关联查询Name*/
    private String collegeName;
    private String specialityName;
    private String className;

}