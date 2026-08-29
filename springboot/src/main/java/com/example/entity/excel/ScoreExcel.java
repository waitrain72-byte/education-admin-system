package com.example.entity.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 成绩 Excel 导出模型
 */
@Data
public class ScoreExcel {

    @ExcelProperty("课程")
    @ColumnWidth(25)
    private String courseName;

    @ExcelProperty("学生")
    @ColumnWidth(15)
    private String studentName;

    @ExcelProperty("教师")
    @ColumnWidth(15)
    private String teacherName;

    @ExcelProperty("平时分")
    @ColumnWidth(10)
    private Double ordinaryScore;

    @ExcelProperty("考试分")
    @ColumnWidth(10)
    private Double examScore;

    @ExcelProperty("总成绩")
    @ColumnWidth(10)
    private Double score;
}
