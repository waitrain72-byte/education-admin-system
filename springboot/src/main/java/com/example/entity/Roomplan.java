package com.example.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 教室安排表
*/
@Data
public class Roomplan implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Integer id;
    /** 教室编号（101–501 或 篮球馆1 等场馆名） */
    private String code;
    /** 教室名称 */
    private String name;
    /** 教室类型：固定占用（办公/器材/杂物，不参与排课）/ 授课教室 / 运动场馆 */
    private String type;
    /** 教室状态（空闲/占用，展示用；排课占用以课程表为准） */
    private String status;
    /** 容纳人数 */
    private Integer num;
    /** 使用说明 */
    private String content;


}
