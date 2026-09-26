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
    /** 发布时间（新增时由后端生成） */
    private String time;
    /** 考试时间（yyyy-MM-dd HH:mm），前端据此显示考试倒计时；历史数据可能为空 */
    private String examTime;
}