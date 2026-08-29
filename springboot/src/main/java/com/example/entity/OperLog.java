package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 操作日志（AOP 切面自动记录所有非 GET 的 Controller 请求）
 */
@Data
public class OperLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    /** 操作人账号 */
    private String username;
    /** 操作模块（类名#方法名） */
    private String module;
    /** 请求方式（POST/PUT/DELETE） */
    private String type;
    /** 请求地址 */
    private String url;
    /** 请求参数（截断至 500 字符，密码已脱敏） */
    private String params;
    /** 操作人 IP */
    private String ip;
    /** 响应码 */
    private String code;
    /** 响应消息 */
    private String msg;
    /** 耗时（毫秒） */
    private Long duration;
    /** 操作时间 */
    private String createTime;
}
