package com.example.service;

import com.example.entity.LoginLog;
import com.example.exception.CustomException;
import com.example.mapper.LoginLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /** 命名为 LOG 而非 log，避免与 saveLog 内的 LoginLog 局部变量重名 */
    private static final Logger LOG = LoggerFactory.getLogger(LoginProtectService.class);

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

    /**
     * 记录一次「账号或密码错误」，累加失败次数，达到阈值则锁定。
     * 计数与锁定时间的更新在 compute 的映射函数内完成，保证并发下的原子性。
     */
    public void recordFailure(String username, String ip, String reason) {
        failMap.compute(username, (k, info) -> {
            FailInfo current = info == null ? new FailInfo() : info;
            current.count++;
            if (current.count >= MAX_ATTEMPTS) {
                current.lockedUntil = System.currentTimeMillis() + LOCK_MILLIS;
            }
            return current;
        });
        saveLog(username, ip, "失败", reason);
    }

    /**
     * 记录一次验证码错误：只写登录日志，不累加账号失败次数。
     *
     * <p>验证码错误与「猜密码」不是同一回事——若也计入锁定计数，任何知道用户名的人
     * 都能通过反复提交错误验证码把该账号锁死 10 分钟，形成拒绝服务。</p>
     */
    public void recordCaptchaFailure(String username, String ip) {
        saveLog(username, ip, "失败", "验证码错误");
    }

    private void saveLog(String username, String ip, String status, String msg) {
        try {
            LoginLog log = new LoginLog();
            log.setUsername(username);
            log.setIp(ip);
            log.setStatus(status);
            log.setMsg(msg);
            loginLogMapper.insert(log);
        } catch (Exception e) {
            // 日志写入失败不影响登录流程，但要留痕，否则登录日志缺失时无从排查
            LOG.warn("登录日志写入失败: username={}, {}", username, e.getMessage());
        }
    }
}
