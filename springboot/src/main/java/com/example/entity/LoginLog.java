package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录日志（登录成功/失败均记录，失败次数用于账号锁定）
 */
@Data
public class LoginLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    /** 登录账号 */
    private String username;
    /** 登录 IP */
    private String ip;
    /** 状态：成功 / 失败 */
    private String status;
    /** 说明（失败原因等） */
    private String msg;
    /** 时间 */
    private String createTime;
}
