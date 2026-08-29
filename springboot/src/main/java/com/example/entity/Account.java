package com.example.entity;

import lombok.Data;

/**
 * 角色用户父类（基类存储基本信息）
 */
@Data
public class Account {
    private Integer id;
    /** 用户名 */
    private String username;
    /** 名称 */
    private String name;
    /** 密码 */
    private String password;
    /** 角色标识 */
    private String role;
    /** 新密码 */
    private String newPassword;
    /** 头像 */
    private String avatar;

    private String token;

    private String captcha;

    /** 主题偏好：light / dark / system（跟随系统） */
    private String theme;

    /** 界面语言：zh-CN / en-US */
    private String locale;

}
