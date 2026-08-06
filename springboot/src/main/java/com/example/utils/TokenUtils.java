package com.example.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.common.Constants;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.service.AdminService;
import com.example.service.StudentService;
import com.example.service.TeacherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * Token 工具类：使用独立配置的签名密钥（不再使用数据库密码作为密钥），
 * 用户修改密码不会导致已签发的 token 全部失效。
 */
@Component
public class TokenUtils {

    private static final Logger log = LoggerFactory.getLogger(TokenUtils.class);

    /** 签名密钥与过期时间（由配置文件注入） */
    private static String jwtSecret;
    private static long jwtExpireHours;

    private static AdminService staticAdminService;
    private static TeacherService staticTeacherService;
    private static StudentService staticStudentService;

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expire-hours:2}")
    private long expireHours;

    @Resource
    AdminService adminService;
    @Resource
    TeacherService teacherService;
    @Resource
    StudentService studentService;

    @PostConstruct
    public void init() {
        jwtSecret = secret;
        jwtExpireHours = expireHours;
        staticAdminService = adminService;
        staticTeacherService = teacherService;
        staticStudentService = studentService;
    }

    public static String getSecret() {
        return jwtSecret;
    }

    /**
     * 生成 token：payload 为 userId-role，使用配置密钥签名并设置过期时间。
     */
    public static String createToken(String data) {
        return JWT.create()
                .withAudience(data)
                .withExpiresAt(DateUtil.offsetHour(new Date(), (int) jwtExpireHours))
                .sign(Algorithm.HMAC256(jwtSecret));
    }

    /**
     * 校验 token 签名与有效期，返回 userId-role 载荷；非法或过期返回 null
     */
    public static String verifyToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        try {
            DecodedJWT decoded = JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(token);
            return decoded.getAudience().get(0);
        } catch (Exception e) {
            log.warn("token 校验失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据 userId-role 查询对应角色的账号（用于校验账号仍存在）
     */
    public static Account getAccountById(Integer userId, String role) {
        if (userId == null || StrUtil.isBlank(role)) {
            return null;
        }
        if (RoleEnum.ADMIN.name().equals(role)) {
            return staticAdminService.selectById(userId);
        }
        if (RoleEnum.TEACHER.name().equals(role)) {
            return staticTeacherService.selectById(userId);
        }
        if (RoleEnum.STUDENT.name().equals(role)) {
            return staticStudentService.selectById(userId);
        }
        return null;
    }

    /**
     * 获取当前登录的用户信息（基于请求头 token）
     */
    public static Account getCurrentUser() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String token = request.getHeader(Constants.TOKEN);
            if (ObjectUtil.isNotEmpty(token)) {
                String userRole = verifyToken(token);
                if (StrUtil.isBlank(userRole)) {
                    return new Account();
                }
                String[] parts = userRole.split("-");
                if (parts.length == 2) {
                    return getAccountById(Integer.valueOf(parts[0]), parts[1]);
                }
            }
        } catch (Exception e) {
            log.error("获取当前用户信息出错", e);
        }
        return new Account();  // 返回空的账号对象
    }
}
