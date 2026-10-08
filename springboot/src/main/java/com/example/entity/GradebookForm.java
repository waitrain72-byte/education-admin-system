package com.example.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 成绩册保存的请求体：四项权重 + 若干学生的平时分、期末分。
 *
 * <p>rows 里出现的学生按传入值覆盖（传空即清空这一项），没出现的学生保持原值；
 * weights 不传则沿用课程现有权重。</p>
 */
@Data
public class GradebookForm implements Serializable {
    private static final long serialVersionUID = 1L;

    private Weights weights;
    private List<Row> rows;

    /** 四项权重（%），合计 100 */
    @Data
    public static class Weights implements Serializable {
        private static final long serialVersionUID = 1L;

        private Integer attendance;
        private Integer homework;
        private Integer ordinary;
        private Integer exam;
    }

    /** 一个学生老师手录的两项成绩（考勤分、作业分按记录自动折算，不由前端传） */
    @Data
    public static class Row implements Serializable {
        private static final long serialVersionUID = 1L;

        private Integer studentId;
        private Double ordinaryScore;
        private Double examScore;
    }
}
