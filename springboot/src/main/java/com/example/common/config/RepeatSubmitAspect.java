package com.example.common.config;

import com.example.common.annotation.NoRepeatSubmit;
import com.example.entity.Account;
import com.example.exception.CustomException;
import com.example.utils.TokenUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 防重复提交切面：以"用户 + 接口方法 + 请求内容"为键，interval 毫秒内的重复调用直接拒绝。
 * 同一份内容连点两次算重复提交；内容不同（连着审批两条不同的请假、给两份作业打分）是两次正常操作，不拦。
 * 业务异常（如账号重复）会移除记录，允许用户立即修正后重试。
 */
@Aspect
@Component
public class RepeatSubmitAspect {

    private static final Map<String, Long> LAST_SUBMIT_TIME = new ConcurrentHashMap<>();

    /** 键里带了请求内容的指纹，记录会越攒越多：超过这个数就顺手清掉早已过了间隔的 */
    private static final int PRUNE_THRESHOLD = 1024;
    private static final long STALE_MS = 60_000L;

    private static final ObjectMapper JSON = new ObjectMapper();

    @Around("@annotation(noRepeat)")
    public Object around(ProceedingJoinPoint joinPoint, NoRepeatSubmit noRepeat) throws Throwable {
        String key = buildKey(joinPoint);
        long now = System.currentTimeMillis();
        prune(now);
        // put 原子地换上新时间并取回旧时间：两个相同请求同时到达时只有一个能通过
        Long last = LAST_SUBMIT_TIME.put(key, now);
        if (last != null && now - last < noRepeat.interval()) {
            throw new CustomException("4090", "操作过于频繁，请稍后再试");
        }
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
        return identity + ":" + joinPoint.getSignature().toShortString() + ":" + fingerprint(joinPoint.getArgs());
    }

    /** 请求参数的指纹（按 JSON 内容算，不依赖实体有没有写 equals/hashCode）；序列化不了的参数只按用户 + 接口判断 */
    static String fingerprint(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        try {
            return Integer.toHexString(JSON.writeValueAsString(args).hashCode());
        } catch (Exception e) {
            return "";
        }
    }

    private static void prune(long now) {
        if (LAST_SUBMIT_TIME.size() > PRUNE_THRESHOLD) {
            LAST_SUBMIT_TIME.values().removeIf(time -> now - time > STALE_MS);
        }
    }
}
