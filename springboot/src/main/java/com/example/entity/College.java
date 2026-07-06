package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 学院信息表
*/
@Data
public class College implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    /** 学院名称 */
    private String name;
    /** 学院介绍 */
    private String content;

}