package com.example.controller;

import com.alibaba.excel.EasyExcel;
import com.example.common.Result;
import com.example.entity.Student;
import com.example.entity.excel.StudentExcel;
import com.example.exception.CustomException;
import com.example.service.StudentService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员前端操作接口
 **/
@RestController
@RequestMapping("/student")
public class StudentController {

    @Resource
    private StudentService studentService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Student student) {
        studentService.add(student);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        studentService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        studentService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Student student) {
        studentService.updateById(student);
        return Result.success();
    }

    @PutMapping("/resetPassword/{id}")
    public Result resetPassword(@PathVariable Integer id) {
        studentService.resetPassword(id);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Student student = studentService.selectById(id);
        return Result.success(student);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Student student ) {
        List<Student> list = studentService.selectAll(student);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Student student,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Student> page = studentService.selectPage(student, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 导出全部学生为 Excel
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("学生列表", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<StudentExcel> rows = studentService.selectAll(new Student()).stream().map(s -> {
            StudentExcel row = new StudentExcel();
            row.setUsername(s.getUsername());
            row.setName(s.getName());
            row.setCollegeName(s.getCollegeName());
            row.setSpecialityName(s.getSpecialityName());
            row.setClassName(s.getClassName());
            row.setScore(s.getScore());
            return row;
        }).collect(Collectors.toList());
        EasyExcel.write(response.getOutputStream(), StudentExcel.class).sheet("学生列表").doWrite(rows);
    }

    /**
     * 下载导入模板（仅表头 + 一行示例说明）
     */
    @GetMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("学生导入模板", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        StudentExcel demo = new StudentExcel();
        demo.setUsername("2024001");
        demo.setName("张三");
        EasyExcel.write(response.getOutputStream(), StudentExcel.class)
                .sheet("学生列表")
                .doWrite(java.util.Collections.singletonList(demo));
    }

    /**
     * 批量导入学生（密码重置为系统默认值，账号重复的行跳过并在结果中说明）
     */
    @PostMapping("/import")
    public Result importExcel(@RequestParam("file") MultipartFile file) throws Exception {
        List<StudentExcel> rows = EasyExcel.read(file.getInputStream())
                .head(StudentExcel.class)
                .sheet()
                .doReadSync();
        if (rows == null || rows.isEmpty()) {
            return Result.error("400", "导入文件为空");
        }
        int ok = 0;
        StringBuilder failed = new StringBuilder();
        for (StudentExcel row : rows) {
            try {
                if (row.getUsername() == null || row.getUsername().trim().isEmpty()) {
                    throw new CustomException("400", "账号为空");
                }
                Student student = new Student();
                student.setUsername(row.getUsername().trim());
                student.setName(row.getName() == null ? row.getUsername().trim() : row.getName().trim());
                student.setRole("STUDENT");
                studentService.add(student);
                ok++;
            } catch (CustomException e) {
                failed.append(row.getUsername()).append("：").append(e.getMessage()).append("；");
            } catch (Exception e) {
                failed.append(row.getUsername()).append("：导入异常；");
            }
        }
        String msg = "成功导入 " + ok + " 条";
        if (failed.length() > 0) {
            msg += "，失败 " + (rows.size() - ok) + " 条：" + failed;
        }
        return Result.success(msg);
    }

}
