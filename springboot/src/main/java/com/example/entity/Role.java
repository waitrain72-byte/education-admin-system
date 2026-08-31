package com.example.entity;

import java.io.Serializable;
import java.util.List;

/**
 * 角色（sys_role）：ADMIN / TEACHER / STUDENT。
 * 角色本身固定三种（与账号表的角色字段对应），可调整的是角色拥有的权限点集合。
 */
public class Role implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    /** 角色标识，与账号表 role 字段取值一致（ADMIN/TEACHER/STUDENT） */
    private String code;
    /** 展示名（中文） */
    private String name;
    /** 角色说明 */
    private String descr;

    /** 该角色拥有的权限码集合（关联查询，非表字段），用于权限设置页回显 */
    private List<String> permissions;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
