package com.example.common.config;

import com.example.common.annotation.RequirePermission;
import com.example.common.enums.ResultCodeEnum;
import com.example.exception.CustomException;
import com.example.service.PermissionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import java.lang.reflect.Method;

/**
 * 权限校验切面（RBAC）：拦截 com.example.controller 下所有控制器方法，
 * 解析其所需的权限码并交由 {@link PermissionService} 判定（权限数据存于
 * sys_permission / sys_role_permission 表，可在权限设置页在线调整）。
 *
 * <p>权限码解析规则（与 {@link RequirePermission} 的用法对应）：</p>
 * <ol>
 *   <li>方法上有 {@code @RequirePermission("xxx:yyy")}：直接使用该权限码；</li>
 *   <li>类上有 {@code @RequirePermission("xxx:yyy")}：该控制器所有接口统一要求该权限码
 *       （不区分读写，适合整页管理员专属的模块，如权限设置）；</li>
 *   <li>方法上无 value 但类上有 {@code @RequirePermission(module = "xxx")}：按请求方式推导——
 *       {@code @GetMapping} 推导 {@code xxx:view}，POST/PUT/DELETE 推导 {@code xxx:manage}，
 *       使 {@link com.example.controller.CrudController} 继承来的通用增删改查端点也能被覆盖；</li>
 *   <li>皆无：该接口不鉴权（仍受 JWT 登录拦截器保护）。</li>
 * </ol>
 */
@Aspect
@Component
public class PermissionAspect {

    @Resource
    private PermissionService permissionService;

    /**
     * 与全局异常处理器的扫描范围保持一致，覆盖全部控制器
     */
    @Around("execution(* com.example.controller..*(..))")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequirePermission methodPerm = AnnotatedElementUtils.findMergedAnnotation(method, RequirePermission.class);
        // 注意用实际调用目标类取类级注解：继承自 CrudController 的方法其 declaringClass 是基类
        RequirePermission classPerm = AnnotatedElementUtils.findMergedAnnotation(joinPoint.getTarget().getClass(), RequirePermission.class);

        String permissionCode;
        if (methodPerm != null && !methodPerm.value().isEmpty()) {
            // 1. 方法级完整权限码优先
            permissionCode = methodPerm.value();
        } else if (classPerm != null && !classPerm.value().isEmpty()) {
            // 2. 类级完整权限码：该控制器所有接口统一要求（不区分读写，适合整页管理员专属的模块）
            permissionCode = classPerm.value();
        } else if (classPerm != null && !classPerm.module().isEmpty()) {
            // 3. 类级模块声明：按请求方式推导 view / manage
            boolean isRead = AnnotatedElementUtils.findMergedAnnotation(method, GetMapping.class) != null;
            permissionCode = classPerm.module() + ":" + (isRead ? "view" : "manage");
        } else {
            return joinPoint.proceed();
        }

        if (!permissionService.hasPermission(permissionCode)) {
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        return joinPoint.proceed();
    }
}
