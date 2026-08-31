package com.example.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限标注（RBAC）：声明访问该接口所需的权限码，由 {@link com.example.common.config.PermissionAspect} 统一校验。
 *
 * <p>两种用法：</p>
 * <ul>
 *   <li>方法级：{@code @RequirePermission("score:manage")} —— 精确声明该接口所需权限码；</li>
 *   <li>类级：{@code @RequirePermission(module = "score")} —— 为控制器声明所属模块，
 *       接口未单独标注时自动推导权限码：读取接口（@GetMapping）推导 {@code 模块:view}，
 *       写入接口（POST/PUT/DELETE 等）推导 {@code 模块:manage}，
 *       使 {@link com.example.controller.CrudController} 继承来的通用增删改查端点也能被覆盖到。</li>
 * </ul>
 *
 * <p>权限码与角色的授权关系存于 sys_role / sys_permission / sys_role_permission 表，
 * 可在【权限设置】页面在线调整；超级管理员（ADMIN）在切面中放行，不依赖授权数据。</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /** 方法级：完整权限码，如 "score:manage" */
    String value() default "";

    /** 类级：所属模块名（对应权限码前缀），如 "score" */
    String module() default "";
}
