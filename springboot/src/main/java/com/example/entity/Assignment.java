package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 作业任务：老师在课程空间里布置，学生按任务提交（提交记录仍存 homework 表，assignment_id 关联）
 */
@Data
public class Assignment implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer courseId;
    private Integer teacherId;
    private String title;
    /** 作业要求 */
    private String content;
    /** 附件地址与原文件名（可空） */
    private String attachment;
    private String attachmentName;
    /** 截止时间 yyyy-MM-dd HH:mm */
    private String deadline;
    /** 满分，默认 100 */
    private Integer fullScore;
    /** 布置时间 yyyy-MM-dd HH:mm */
    private String createTime;
}
