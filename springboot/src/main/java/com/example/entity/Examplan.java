package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 考试安排表
*/
@Data
public class Examplan implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Integer id;
    /** 标题 */
    private String name;
    /** 内容 */
    private String content;
    /** 时间 */
    private String time;
}