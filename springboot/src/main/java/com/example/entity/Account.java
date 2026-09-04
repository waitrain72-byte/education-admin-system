package com.example.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    /**
     * 密码：仅允许从请求 JSON 反序列化进来（登录/改密），序列化时永不输出，
     * 防止 selectPage/selectAll/selectById 等查询接口把 BCrypt 哈希泄露给前端。
     * 子类若重新声明 password 字段，需同样标注。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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
