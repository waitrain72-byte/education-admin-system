package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.NoRepeatSubmit;
import com.example.common.annotation.RequirePermission;
import com.example.entity.Course;
import com.example.entity.GradebookForm;
import com.example.service.CourseSpaceService;
import com.example.service.GradebookService;
import com.example.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程空间 · 成绩册。权限沿用成绩模块：查看 score:view，保存 / 发布 / 撤回 score:manage；
 * 具体能不能操作这门课再由 Service 按「是不是这门课的任课教师」判断。
 */
@RestController
@RequestMapping("/course/{courseId}/gradebook")
@RequirePermission(module = "score")
public class GradebookController {

    @Resource
    private GradebookService gradebookService;
    @Resource
    private CourseSpaceService courseSpaceService;
    @Resource
    private MessageService messageService;

    /** 老师看的成绩册 */
    @GetMapping
    public Result view(@PathVariable Integer courseId) {
        return Result.success(gradebookService.view(courseId));
    }

    /** 学生看自己的成绩 */
    @GetMapping("/mine")
    public Result mine(@PathVariable Integer courseId) {
        return Result.success(gradebookService.mine(courseId));
    }

    @NoRepeatSubmit
    @PutMapping
    public Result save(@PathVariable Integer courseId, @RequestBody GradebookForm form) {
        GradebookService.Outcome outcome = gradebookService.save(courseId, form);
        notifyStudents(courseId, outcome.getNotify(), "成绩更新通知", "的成绩有更新，点击查看");
        return Result.success(counts(outcome));
    }

    @NoRepeatSubmit
    @PostMapping("/publish")
    public Result publish(@PathVariable Integer courseId) {
        GradebookService.Outcome outcome = gradebookService.publish(courseId);
        notifyStudents(courseId, outcome.getNotify(), "成绩发布通知", "的成绩已发布，点击查看");
        return Result.success(counts(outcome));
    }

    @NoRepeatSubmit
    @PostMapping("/unpublish")
    public Result unpublish(@PathVariable Integer courseId) {
        return Result.success(counts(gradebookService.unpublish(courseId)));
    }

    /** 事务已提交，再逐个通知学生 */
    private void notifyStudents(Integer courseId, List<Integer> studentIds, String title, String suffix) {
        if (studentIds.isEmpty()) {
            return;
        }
        Course course = courseSpaceService.requireCourse(courseId);
        String link = "/course/" + courseId + "/grades";
        for (Integer studentId : studentIds) {
            messageService.push(studentId, "STUDENT", "score", title, "《" + course.getName() + "》" + suffix, link);
        }
    }

    private static Map<String, Object> counts(GradebookService.Outcome outcome) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", outcome.getCount());
        data.put("skipped", outcome.getSkipped());
        data.put("notified", outcome.getNotify().size());
        return data;
    }
}
