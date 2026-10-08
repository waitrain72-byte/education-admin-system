package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 课程资料：课件、讲义等，老师上传，课程成员可下载
 */
@Data
public class CourseResource implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer courseId;
    /** 资料名称（原文件名） */
    private String name;
    /** 文件地址（/api/files/xxx） */
    private String file;
    /** 字节数 */
    private Long size;
    private Integer uploaderId;
    private String uploaderRole;
    /** yyyy-MM-dd HH:mm */
    private String createTime;
    /** 关联查询：上传人姓名 */
    private String uploaderName;
}
