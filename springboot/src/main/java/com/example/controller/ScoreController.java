package com.example.controller;

import com.alibaba.excel.EasyExcel;
import com.example.common.Result;
import com.example.common.annotation.NoRepeatSubmit;
import com.example.common.annotation.RequirePermission;
import com.example.entity.Score;
import com.example.entity.excel.ScoreExcel;
import com.example.service.CrudService;
import com.example.service.ScoreService;
import com.example.websocket.NoticeWebSocketServer;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成绩信息表前端操作接口（通用增删改查见 {@link CrudController}）
 **/
@RestController
@RequestMapping("/score")
@RequirePermission(module = "score")
public class ScoreController extends CrudController<Score> {

    @Resource
    private ScoreService scoreService;

    @Override
    protected CrudService<Score> getService() {
        return scoreService;
    }

    /**
     * 新增（发布成绩实时推送学生，防重复提交）
     */
    @Override
    @NoRepeatSubmit
    @PostMapping("/add")
    public Result add(@RequestBody Score score) {
        scoreService.add(score);
        NoticeWebSocketServer.sendToUser(score.getStudentId(), "STUDENT",
                "成绩发布通知", "你有一门课程的成绩已发布，请到【我的成绩】查看");
        return Result.success();
    }

    /**
     * 修改（修改成绩实时推送学生，防重复提交）
     */
    @Override
    @NoRepeatSubmit
    @PutMapping("/update")
    public Result updateById(@RequestBody Score score) {
        scoreService.updateById(score);
        NoticeWebSocketServer.sendToUser(score.getStudentId(), "STUDENT",
                "成绩发布通知", "你有一门课程的成绩已更新，请到【我的成绩】查看");
        return Result.success();
    }

    /**
     * 导出全部成绩为 Excel
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("成绩列表", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<ScoreExcel> rows = scoreService.selectAll(new Score()).stream().map(s -> {
            ScoreExcel row = new ScoreExcel();
            row.setCourseName(s.getCourseName());
            row.setStudentName(s.getStudentName());
            row.setTeacherName(s.getTeacherName());
            row.setOrdinaryScore(s.getOrdinaryScore());
            row.setExamScore(s.getExamScore());
            row.setScore(s.getScore());
            return row;
        }).collect(Collectors.toList());
        EasyExcel.write(response.getOutputStream(), ScoreExcel.class).sheet("成绩列表").doWrite(rows);
    }

    /**
     * 首页 Echarts：成绩分布统计（折线图）
     */
    @GetMapping("/getLine")
    public Result getLine() {
        Map<String, Object> resultMap = new HashMap<>();
        List<String> xList = new ArrayList<>();
        List<Long> yList = new ArrayList<>();

        // 封装一下 xList 和 yList
        List<Score> list = scoreService.selectAll(new Score());
        // 优（90分-100分）
        xList.add("优（90分-100分）");
        yList.add(list.stream().filter(x -> x.getScore() >= 90).count());
        // 良（80分-89分）
        xList.add("良（80分-89分）");
        yList.add(list.stream().filter(x -> x.getScore() >= 80 && x.getScore() < 90).count());
        // 中（70分-79分）
        xList.add("中（70分-79分）");
        yList.add(list.stream().filter(x -> x.getScore() >= 70 && x.getScore() < 80).count());
        // 及格（60分-69分）
        xList.add("及格（60分-69分）");
        yList.add(list.stream().filter(x -> x.getScore() >= 60 && x.getScore() < 70).count());
        // 不及格（<60分）
        xList.add("不及格（<60分）");
        yList.add(list.stream().filter(x -> x.getScore() < 60).count());

        resultMap.put("text", "成绩分布统计（折线图）");
        resultMap.put("subtext", "统计维度：成绩段");
        resultMap.put("xAxis", xList);
        resultMap.put("yAxis", yList);
        return Result.success(resultMap);
    }
}
