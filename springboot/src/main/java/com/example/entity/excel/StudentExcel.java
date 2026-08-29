package com.example.entity.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 学生 Excel 导入导出模型
 */
@Data
public class StudentExcel {

    @ExcelProperty("账号")
    @ColumnWidth(20)
    private String username;

    @ExcelProperty("姓名")
    @ColumnWidth(15)
    private String name;

    @ExcelProperty("学院")
    @ColumnWidth(25)
    private String collegeName;

    @ExcelProperty("专业")
    @ColumnWidth(25)
    private String specialityName;

    @ExcelProperty("班级")
    @ColumnWidth(20)
    private String className;

    @ExcelProperty("学分")
    @ColumnWidth(10)
    private Integer score;
}
