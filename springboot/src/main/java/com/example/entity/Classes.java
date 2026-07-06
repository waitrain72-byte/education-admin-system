package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 班级信息表
*/
@Data
public class Classes implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    /** 班级名称 */
    private String name;
    /** 班级描述 */
    private String content;
    /** 教师ID */
    private Integer teacherId;
    /** 专业ID */
    private Integer specialityId;


    private String teacherName;
    private String specialityName;                                  /*关联查询*/

}