package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.NoRepeatSubmit;
import com.example.common.annotation.RequirePermission;
import com.example.entity.Assignment;
import com.example.entity.Course;
import com.example.entity.Homework;
import com.example.service.AssignmentService;
import com.example.service.CourseSpaceService;
import com.example.service.MessageService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 课程空间 · 作业。权限：查看 assignment:view，布置 / 修改 / 删除 / 批改 assignment:manage，
 * 学生提交 assignment:submit；能不能操作这门课再由 Service 按与课程的关系判断。
 */
@RestController
@RequestMapping("/course/{courseId}/assignments")
@RequirePermission(module = "assignment")
public class AssignmentController {

    @Resource
    private AssignmentService assignmentService;
    @Resource
    private CourseSpaceService courseSpaceService;
    @Resource
    private MessageService messageService;

    @GetMapping
    public Result list(@PathVariable Integer courseId) {
        return Result.success(assignmentService.list(courseId));
    }

    /** 旧版自由提交（没挂在作业任务下）的作业 */
    @GetMapping("/loose")
    public Result loose(@PathVariable Integer courseId) {
        return Result.success(assignmentService.looseSubmissions(courseId));
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Integer courseId, @PathVariable Integer id) {
        return Result.success(assignmentService.detail(courseId, id));
    }

    /** 布置作业，并通知全体选课学生 */
    @NoRepeatSubmit
    @PostMapping
    public Result create(@PathVariable Integer courseId, @RequestBody Assignment form) {
        Assignment saved = assignmentService.create(courseId, form);
        Course course = courseSpaceService.requireCourse(courseId);
        courseSpaceService.notifyStudents(courseId, "homework", "《" + course.getName() + "》布置了新作业",
                saved.getTitle() + "，截止时间 " + saved.getDeadline(), assignmentLink(courseId, saved.getId()));
        courseSpaceService.emit(courseId, "assignments");
        return Result.success(saved);
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Integer courseId, @PathVariable Integer id, @RequestBody Assignment form) {
        Assignment saved = assignmentService.update(courseId, id, form);
        courseSpaceService.emit(courseId, "assignments");
        return Result.success(saved);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer courseId, @PathVariable Integer id) {
        assignmentService.delete(courseId, id);
        courseSpaceService.emit(courseId, "assignments");
        return Result.success();
    }

    /** 学生提交作业：body 为 {content, file, fileName} */
    @NoRepeatSubmit
    @RequirePermission("assignment:submit")
    @PostMapping("/{id}/submit")
    public Result submit(@PathVariable Integer courseId, @PathVariable Integer id, @RequestBody Homework form) {
        Homework saved = assignmentService.submit(courseId, id, form);
        courseSpaceService.emitToTeacher(courseId, "assignments");
        return Result.success(saved);
    }

    /** 批改：body 为 {score, descr}，批改结果通知学生 */
    @PutMapping("/submissions/{homeworkId}")
    public Result grade(@PathVariable Integer courseId, @PathVariable Integer homeworkId,
                        @RequestBody Map<String, Object> body) {
        Object score = body == null ? null : body.get("score");
        Object comment = body == null ? null : body.get("descr");
        Homework graded = assignmentService.grade(courseId, homeworkId,
                score instanceof Number ? ((Number) score).doubleValue() : parse(score),
                comment == null ? null : String.valueOf(comment));

        String title = "作业已批改";
        String link = "/course/" + courseId + "/assignments";
        String content = "你的作业得分 " + graded.getScore();
        if (graded.getAssignmentId() != null) {
            Assignment assignment = assignmentService.requireAssignment(courseId, graded.getAssignmentId());
            content = "《" + assignment.getTitle() + "》得分 " + graded.getScore() + " / " + assignment.getFullScore();
            link = assignmentLink(courseId, assignment.getId());
        }
        messageService.push(graded.getStudentId(), "STUDENT", "homework", title, content, link);
        courseSpaceService.emit(courseId, "assignments");
        return Result.success(graded);
    }

    private static String assignmentLink(Integer courseId, Integer assignmentId) {
        return "/course/" + courseId + "/assignments?open=" + assignmentId;
    }

    private static Double parse(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
