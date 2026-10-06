package com.example.entity;

import lombok.Data;

/**
 * 学期设置（存于 sys_config 键值表，这里是接口出入参）
 */
@Data
public class SemesterConfig {
    /** 学期名称，如「2026-2027 学年第一学期」 */
    private String name;
    /** 开学日期（第 1 教学周的周一），yyyy-MM-dd */
    private String startDate;
    /** 教学周数 */
    private Integer weeks;
}
