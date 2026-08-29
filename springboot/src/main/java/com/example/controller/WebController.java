package com.example.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.Result;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.service.AdminService;
import com.example.service.StudentService;
import com.example.service.TeacherService;
import com.example.utils.TokenUtils;
import com.wf.captcha.SpecCaptcha;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/**
 * 基础前端接口
 */
@RestController
public class WebController {

    @Resource
    private AdminService adminService;
    @Resource
    private TeacherService teacherService;
    @Resource
    private StudentService studentService;

    @GetMapping("/")
    public Result hello() {
        return Result.success("访问成功");
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody Account account, HttpServletRequest request) {
        if (ObjectUtil.isEmpty(account.getUsername()) || ObjectUtil.isEmpty(account.getPassword())
                || ObjectUtil.isEmpty(account.getRole())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }

        // 验证码校验
        String sessionCaptcha = (String) request.getSession().getAttribute("captcha");
        if (ObjectUtil.isEmpty(account.getCaptcha()) || !account.getCaptcha().toLowerCase().equals(sessionCaptcha)) {
            return Result.error(ResultCodeEnum.CAPTCHA_ERROR);
        }

        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            account = adminService.login(account);
        }
        if (RoleEnum.TEACHER.name().equals(account.getRole())) {
            account = teacherService.login(account);
        }
        if (RoleEnum.STUDENT.name().equals(account.getRole())) {
            account = studentService.login(account);
        }

        return Result.success(account);
    }

    /**
     * 验证码
     */
    @GetMapping("/captcha")
    public void captcha(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("image/gif");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");

        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        request.getSession().setAttribute("captcha", captcha.text().toLowerCase());
        captcha.out(response.getOutputStream());
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result register(@RequestBody Account account) {
        if (StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getPassword())
                || ObjectUtil.isEmpty(account.getRole())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (RoleEnum.STUDENT.name().equals(account.getRole())) {
            studentService.register(account);
        }
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody Account account) {
        if (StrUtil.isBlank(account.getUsername()) || StrUtil.isBlank(account.getPassword())
                || ObjectUtil.isEmpty(account.getNewPassword())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            adminService.updatePassword(account);
        }
        if (RoleEnum.TEACHER.name().equals(account.getRole())) {
            teacherService.updatePassword(account);
        }
        if (RoleEnum.STUDENT.name().equals(account.getRole())) {
            studentService.updatePassword(account);
        }
        return Result.success();
    }

    /**
     * 主题偏好取值白名单：防注入的第一道防线是 #{} 预编译，
     * 白名单校验进一步保证只有合法枚举值能落库
     */
    private static final List<String> THEME_VALUES = Arrays.asList("light", "dark", "system");

    /**
     * 查询当前登录用户的主题偏好（供新终端登录时同步，实现"一次设置，多端同步"）
     */
    @GetMapping("/theme")
    public Result getTheme() {
        Account current = TokenUtils.getCurrentUser();
        if (ObjectUtil.isEmpty(current.getId())) {
            return Result.error(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        String theme = current.getTheme();
        return Result.success(ObjectUtil.isEmpty(theme) ? "system" : theme);
    }

    /**
     * 保存当前登录用户的主题偏好
     */
    @PutMapping("/theme")
    public Result updateTheme(@RequestBody Account account) {
        if (ObjectUtil.isEmpty(account.getTheme()) || !THEME_VALUES.contains(account.getTheme())) {
            return Result.error(ResultCodeEnum.PARAM_ERROR);
        }
        Account current = TokenUtils.getCurrentUser();
        if (ObjectUtil.isEmpty(current.getId())) {
            return Result.error(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        account.setId(current.getId());
        if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            adminService.updateTheme(account);
        } else if (RoleEnum.TEACHER.name().equals(current.getRole())) {
            teacherService.updateTheme(account);
        } else if (RoleEnum.STUDENT.name().equals(current.getRole())) {
            studentService.updateTheme(account);
        } else {
            return Result.error(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        return Result.success();
    }

}
