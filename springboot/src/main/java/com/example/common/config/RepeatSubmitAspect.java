package com.example.common.config;

import com.example.common.annotation.NoRepeatSubmit;
import com.example.entity.Account;
import com.example.exception.CustomException;
import com.example.utils.TokenUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 防重复提交切面：以"用户 + 接口方法"为键，interval 毫秒内的重复调用直接拒绝。
 * 业务异常（如账号重复）会移除记录，允许用户立即修正后重试。
 */
@Aspect
@Component
public class RepeatSubmitAspect {

    private static final Map<String, Long> LAST_SUBMIT_TIME = new ConcurrentHashMap<>();

    @Around("@annotation(noRepeat)")
    public Object around(ProceedingJoinPoint joinPoint, NoRepeatSubmit noRepeat) throws Throwable {
        String key = buildKey(joinPoint);
        long now = System.currentTimeMillis();
        Long last = LAST_SUBMIT_TIME.get(key);
        if (last != null && now - last < noRepeat.interval()) {
            throw new CustomException("4090", "操作过于频繁，请稍后再试");
        }
        LAST_SUBMIT_TIME.put(key, now);
        try {
            return joinPoint.proceed();
        } catch (RuntimeException e) {
            // 提交失败时解除限制，允许立即修正后重试
            LAST_SUBMIT_TIME.remove(key);
            throw e;
        }
    }

    private String buildKey(ProceedingJoinPoint joinPoint) {
        Account user = TokenUtils.getCurrentUser();
        String identity = (user != null && user.getId() != null)
                ? user.getId() + "-" + user.getRole()
                : "anonymous";
        return identity + ":" + joinPoint.getSignature().toShortString();
    }
}
