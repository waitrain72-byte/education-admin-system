package com.example.entity;

import java.io.Serializable;

/**
 * 权限点（sys_permission）：一个权限码对应一条记录，如 score:manage。
 * type 取值：menu（菜单/页面可见性）/ button（按钮/接口级操作）。
 */
public class Permission implements Serializable {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Integer id;
    /** 权限码，全局唯一，格式 模块:动作（如 course:manage） */
    private String code;
    /** 展示名（中文），供权限设置页显示 */
    private String name;
    /** 类型：menu=页面级，button=接口/按钮级 */
    private String type;
    /** 所属模块（权限码前缀），供权限设置页分组展示 */
    private String module;
    /** 排序号，权限设置页内同模块内排序 */
    private Integer sortNum;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public Integer getSortNum() {
        return sortNum;
    }

    public void setSortNum(Integer sortNum) {
        this.sortNum = sortNum;
    }
}
