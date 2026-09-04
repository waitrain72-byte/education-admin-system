package com.example.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 教师
 */
@Data
public class Teacher extends Account implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    private String username;
    /** 密码（同基类：只进不出，防止查询接口泄露哈希） */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String name;
    private String avatar;
    private String role;
    private String phone;
    private String email;
    private String title;

}