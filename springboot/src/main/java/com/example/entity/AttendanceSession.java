package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 课堂签到场次：老师发起后在截止时间前，学生输入签到码完成签到
 */
@Data
public class AttendanceSession implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_ACTIVE = "进行中";
    public static final String STATUS_ENDED = "已结束";

    private Integer id;
    private Integer courseId;
    private Integer teacherId;
    /** 4 位数字签到码 */
    private String code;
    /** 上课日期 yyyy-MM-dd（考勤记录的 time） */
    private String date;
    /** yyyy-MM-dd HH:mm:ss */
    private String startTime;
    private String expireTime;
    /** 进行中 / 已结束 */
    private String status;
}
