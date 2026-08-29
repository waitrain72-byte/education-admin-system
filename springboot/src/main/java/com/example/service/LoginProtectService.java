package com.example.service;

import com.example.entity.LoginLog;
import com.example.exception.CustomException;
import com.example.mapper.LoginLogMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录安全防护：
 * - 记录登录成功/失败日志（sys_login_log）
 * - 同一账号连续失败达到上限后锁定一段时间（内存实现，单实例部署适用）
 */
@Service
public class LoginProtectService {

    /** 连续失败锁定阈值 */
    private static final int MAX_ATTEMPTS = 5;
    /** 锁定时长（毫秒） */
    private static final long LOCK_MILLIS = 10 * 60 * 1000L;

    private final Map<String, FailInfo> failMap = new ConcurrentHashMap<>();

    @Resource
    private LoginLogMapper loginLogMapper;

    private static class FailInfo {
        int count;
        long lockedUntil;
    }

    /**
     * 校验账号是否处于锁定状态，是则抛出异常（GlobalExceptionHandler 统一返回）
     */
    public void checkLocked(String username) {
        FailInfo info = failMap.get(username);
        if (info == null) return;
        long now = System.currentTimeMillis();
        if (info.lockedUntil > now) {
            long minutes = (info.lockedUntil - now) / 60000 + 1;
            throw new CustomException("4008", "账号已锁定，请约 " + minutes + " 分钟后再试");
        }
        // 锁定已过期，重置计数
        if (info.count >= MAX_ATTEMPTS) {
            failMap.remove(username);
        }
    }

    public void recordSuccess(String username, String ip) {
        failMap.remove(username);
        saveLog(username, ip, "成功", "登录成功");
    }

    public void recordFailure(String username, String ip, String reason) {
        FailInfo info = failMap.computeIfAbsent(username, k -> new FailInfo());
        info.count++;
        if (info.count >= MAX_ATTEMPTS) {
            info.lockedUntil = System.currentTimeMillis() + LOCK_MILLIS;
        }
        saveLog(username, ip, "失败", reason);
    }

    private void saveLog(String username, String ip, String status, String msg) {
        try {
            LoginLog log = new LoginLog();
            log.setUsername(username);
            log.setIp(ip);
            log.setStatus(status);
            log.setMsg(msg);
            loginLogMapper.insert(log);
        } catch (Exception ignored) {
            // 日志写入失败不影响登录流程
        }
    }
}
