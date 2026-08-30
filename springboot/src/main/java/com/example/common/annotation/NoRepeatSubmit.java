package com.example.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交：标注在写接口上，同一用户在 interval 毫秒内重复调用会被拒绝。
 * 由 RepeatSubmitAspect 切面实现。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoRepeatSubmit {

    /** 两次提交的最小间隔（毫秒），默认 2000 */
    int interval() default 2000;
}
