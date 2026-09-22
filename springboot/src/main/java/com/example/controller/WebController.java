package com.example.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.Result;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.exception.CustomException;
import com.example.service.AdminService;
import com.example.service.LoginProtectService;
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
import java.util.regex.Pattern;

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
    @Resource
    private LoginProtectService loginProtectService;

    @GetMapping("/")
    public Result hello() {
        return Result.success("访问成功");
    }

    /**
     * 登录（带验证码校验、连续失败锁定与登录日志）
     */
    @PostMapping("/login")
    public Result login(@RequestBody Account account, HttpServletRequest request) {
        if (ObjectUtil.isEmpty(account.getUsername()) || ObjectUtil.isEmpty(account.getPassword())
                || ObjectUtil.isEmpty(account.getRole())) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }

        String ip = request.getRemoteAddr();

        // 连续失败锁定校验：先于验证码校验，已锁定的账号直接短路，不必再消耗验证码
        loginProtectService.checkLocked(account.getUsername());

        // 验证码校验：读取后立即失效，做到一码一用（否则同一张验证码可反复提交用于爆破密码）
        String sessionCaptcha = (String) request.getSession().getAttribute("captcha");
        request.getSession().removeAttribute("captcha");
        if (ObjectUtil.isEmpty(account.getCaptcha()) || !account.getCaptcha().toLowerCase().equals(sessionCaptcha)) {
            // 验证码错误不计入账号失败次数：否则知道用户名的人反复提交错误验证码即可把该账号锁死（DoS）
            loginProtectService.recordCaptchaFailure(account.getUsername(), ip);
            return Result.error(ResultCodeEnum.CAPTCHA_ERROR);
        }

        try {
            if (RoleEnum.ADMIN.name().equals(account.getRole())) {
                account = adminService.login(account);
            }
            if (RoleEnum.TEACHER.name().equals(account.getRole())) {
                account = teacherService.login(account);
            }
            if (RoleEnum.STUDENT.name().equals(account.getRole())) {
                account = studentService.login(account);
            }
            loginProtectService.recordSuccess(account.getUsername(), ip);
            return Result.success(account);
        } catch (CustomException e) {
            // 账号或密码错误等业务异常：记录失败日志并向上抛出（全局异常处理器统一返回）
            loginProtectService.recordFailure(account.getUsername(), ip, e.getMessage());
            throw e;
        }
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

    /** 界面语言取值白名单 */
    private static final List<String> LOCALE_VALUES = Arrays.asList("zh-CN", "en-US");

    /**
     * 查询当前登录用户的界面语言偏好
     */
    @GetMapping("/locale")
    public Result getLocale() {
        Account current = TokenUtils.getCurrentUser();
        if (ObjectUtil.isEmpty(current.getId())) {
            return Result.error(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        String locale = current.getLocale();
        return Result.success(ObjectUtil.isEmpty(locale) ? "zh-CN" : locale);
    }

    /**
     * 保存当前登录用户的界面语言偏好
     */
    @PutMapping("/locale")
    public Result updateLocale(@RequestBody Account account) {
        if (ObjectUtil.isEmpty(account.getLocale()) || !LOCALE_VALUES.contains(account.getLocale())) {
            return Result.error(ResultCodeEnum.PARAM_ERROR);
        }
        Account current = TokenUtils.getCurrentUser();
        if (ObjectUtil.isEmpty(current.getId())) {
            return Result.error(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        account.setId(current.getId());
        if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            adminService.updateLocale(account);
        } else if (RoleEnum.TEACHER.name().equals(current.getRole())) {
            teacherService.updateLocale(account);
        } else if (RoleEnum.STUDENT.name().equals(current.getRole())) {
            studentService.updateLocale(account);
        } else {
            return Result.error(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        return Result.success();
    }

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

    /**
     * 自定义主题色格式：#RRGGBB（大小写均可）。
     * 与 theme/locale 的枚举白名单不同，颜色是开放取值，因此用正则限定格式，
     * 只放行 6 位十六进制，杜绝任意字符串落库（前端会把它直接写进 CSS 变量）。
     */
    private static final Pattern THEME_COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");

    /**
     * 查询当前登录用户的自定义主题色（空串表示使用系统内置默认色）
     */
    @GetMapping("/themeColor")
    public Result getThemeColor() {
        Account current = TokenUtils.getCurrentUser();
        if (ObjectUtil.isEmpty(current.getId())) {
            return Result.error(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        String themeColor = current.getThemeColor();
        return Result.success(themeColor == null ? "" : themeColor);
    }

    /**
     * 保存当前登录用户的自定义主题色。
     * 传空串表示恢复默认色（前端「恢复默认」按钮）。
     */
    @PutMapping("/themeColor")
    public Result updateThemeColor(@RequestBody Account account) {
        String color = account.getThemeColor();
        if (color == null) {
            return Result.error(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        // 空串 = 恢复默认色，属于合法输入；非空则必须是 #RRGGBB
        if (!color.isEmpty() && !THEME_COLOR_PATTERN.matcher(color).matches()) {
            return Result.error(ResultCodeEnum.PARAM_ERROR);
        }
        Account current = TokenUtils.getCurrentUser();
        if (ObjectUtil.isEmpty(current.getId())) {
            return Result.error(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        account.setId(current.getId());
        // 统一小写入库，避免同一颜色因大小写不同被当成两个值
        account.setThemeColor(color.toLowerCase());
        if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            adminService.updateThemeColor(account);
        } else if (RoleEnum.TEACHER.name().equals(current.getRole())) {
            teacherService.updateThemeColor(account);
        } else if (RoleEnum.STUDENT.name().equals(current.getRole())) {
            studentService.updateThemeColor(account);
        } else {
            return Result.error(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        return Result.success();
    }

}
