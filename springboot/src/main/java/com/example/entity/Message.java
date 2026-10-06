package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 站内消息：成绩发布、作业批改、请假审核等推送落库后的记录，消息中心可回看
 */
@Data
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    /** 接收人 ID */
    private Integer userId;
    /** 接收人角色（ADMIN / TEACHER / STUDENT），与 userId 一起确定接收人 */
    private String role;
    /** 类型：score / homework / apply / warning / attendance / course */
    private String type;
    private String title;
    private String content;
    /** 点击后跳转的前端路径（可空） */
    private String link;
    private Boolean isRead;
    /** yyyy-MM-dd HH:mm */
    private String createTime;
}
