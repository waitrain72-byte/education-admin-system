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
    /** 教室名称 */
    private String name;
    /** 教室状态 */
    private String status;
    /** 容纳人数 */
    private Integer num;
    /** 使用说明 */
    private String content;


}
