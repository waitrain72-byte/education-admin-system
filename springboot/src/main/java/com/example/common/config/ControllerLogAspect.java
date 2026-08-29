package com.example.common.config;

import cn.hutool.json.JSONUtil;
import com.example.common.Result;
import com.example.entity.Account;
import com.example.entity.OperLog;
import com.example.service.LogService;
import com.example.utils.TokenUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 操作日志切面：自动记录所有非 GET 的 Controller 请求（除登录与日志接口本身），
 * 无需在每个接口上手写埋点。记录失败时静默忽略，不影响业务。
 */
@Aspect
@Component
public class ControllerLogAspect {

    /** 参数最大保留长度，防止大请求体撑爆日志表 */
    private static final int MAX_PARAM_LENGTH = 500;

    @Resource
    private LogService logService;

    @Around("execution(* com.example.controller..*(..)) " +
            "&& !target(com.example.controller.WebController) " +
            "&& !target(com.example.controller.LogController)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        try {
            saveLog(joinPoint, result, System.currentTimeMillis() - start);
        } catch (Exception ignored) {
        }
        return result;
    }

    private void saveLog(ProceedingJoinPoint joinPoint, Object result, long duration) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;
        HttpServletRequest request = attrs.getRequest();

        // 只记录会修改数据的请求（GET 查询不记录）
        String type = request.getMethod();
        if ("GET".equalsIgnoreCase(type)) return;

        OperLog log = new OperLog();

        // 操作人（未登录场景如登录接口已被排除，正常必有 token）
        Account user = TokenUtils.getCurrentUser();
        log.setUsername(user != null && user.getUsername() != null ? user.getUsername() : "unknown");

        // 模块：类名#方法名
        log.setModule(joinPoint.getTarget().getClass().getSimpleName()
                + "#" + joinPoint.getSignature().getName());
        log.setType(type);
        log.setUrl(request.getRequestURI());

        // 参数：文件上传不记录内容，其余 JSON 序列化并脱敏截断
        if (isMultipart(request)) {
            log.setParams("(文件上传)");
        } else {
            log.setParams(maskPassword(JSONUtil.toJsonStr(joinPoint.getArgs())));
        }

        log.setIp(request.getRemoteAddr());

        // 响应码与消息
        if (result instanceof Result) {
            log.setCode(((Result) result).getCode());
            log.setMsg(((Result) result).getMsg());
        }

        log.setDuration(duration);
        logService.insertOper(log);
    }

    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains("multipart/");
    }

    /** 将参数中的密码字段脱敏，并截断超长内容 */
    private String maskPassword(String json) {
        String masked = json.replaceAll(
                "(\"(password|newPassword|confirmPassword)\"\\s*:\\s*\")([^\"]*)(\")",
                "$1***$4");
        if (masked.length() > MAX_PARAM_LENGTH) {
            masked = masked.substring(0, MAX_PARAM_LENGTH) + "...";
        }
        return masked;
    }
}
