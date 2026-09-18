package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.RequirePermission;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.service.CourseService;
import com.example.service.CrudService;
import com.example.service.RecommendService;
import com.example.service.RoomplanService;
import com.example.utils.TokenUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 课程信息表前端操作接口（通用增删改查见 {@link CrudController}）
 **/
@RestController
@RequestMapping("/course")
@RequirePermission(module = "course")
public class CourseController extends CrudController<Course> {

    @Resource
    private CourseService courseService;

    @Resource
    private RecommendService recommendService;

    @Resource
    private RoomplanService roomplanService;

    @Override
    protected CrudService<Course> getService() {
        return courseService;
    }

    /**
     * 课程推荐（基于物品的协同过滤）：为当前登录用户生成个性化推荐，
     * 学生按选课相似度推荐，无选课记录（冷启动）降级为按选课人数的热门推荐
     */
    @GetMapping("/recommend")
    public Result recommend(@RequestParam(defaultValue = "5") int limit) {
        Account current = TokenUtils.getCurrentUser();
        int safeLimit = Math.max(1, Math.min(limit, 10));
        return Result.success(recommendService.recommendForStudent(current.getId(), safeLimit));
    }

    /**
     * 教室占用查询：返回同一「教室 + 星期 + 大节」已存在的课程（供表单在分配教室前实时提示），无占用返回 null
     */
    @GetMapping("/roomOccupied")
    public Result roomOccupied(@RequestParam String room,
                               @RequestParam String week,
                               @RequestParam String segment,
                               @RequestParam(required = false) Integer excludeId) {
        return Result.success(courseService.selectRoomOccupied(room, week, segment, excludeId));
    }

    /**
     * 空闲教室查询：可排课类型（授课教室/运动场馆）中，未被非结课课程占用
     * 「week + segment」时段、容纳人数 >= num 的教室，按容量贴近度排序。
     * 表单教室下拉的数据源；取列表第一条即为「系统自动分配」的结果。
     *
     * @param typeFilter 可选，限定类型（体育类课程传 运动场馆 以优先分配场馆）
     */
    @GetMapping("/roomFree")
    public Result roomFree(@RequestParam String week,
                           @RequestParam String segment,
                           @RequestParam(required = false) Integer num,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String typeFilter,
                           @RequestParam(required = false) Integer excludeId) {
        return Result.success(roomplanService.selectFreeRooms(week, segment, num, keyword, typeFilter, excludeId));
    }
}
