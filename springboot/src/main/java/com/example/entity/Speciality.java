package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 专业信息表
*/
@Data
public class Speciality implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    /** 专业名称 */
    private String name;
    /** 专业描述 */
    private String content;
    /** 所属学院 */
    private Integer collegeId;
    /** 学分 */
    private Integer score;

    private String collegeName;            /* 关联查询*/

}