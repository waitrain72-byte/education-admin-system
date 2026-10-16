package com.example.service;

import com.example.entity.LoginLog;
import com.example.exception.CustomException;
import com.example.mapper.LoginLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 登录保护：同一账号 15 分钟内连续输错 5 次密码锁 10 分钟；验证码错误不计数；成功或锁定到期后重新计数。
 * 用固定时钟一步步拨时间，不真的等待。
 */
class LoginProtectServiceTest {

    private static final Instant START = Instant.parse("2026-10-15T02:00:00Z");
    private static final String IP = "127.0.0.1";

    private LoginProtectService service;
    private LoginLogMapper loginLogMapper;

    @BeforeEach
    void setUp() {
        service = new LoginProtectService();
        loginLogMapper = mock(LoginLogMapper.class);
        ReflectionTestUtils.setField(service, "loginLogMapper", loginLogMapper);
        at(0);
    }

    /** 把时钟拨到起点之后 seconds 秒 */
    private void at(long seconds) {
        ReflectionTestUtils.setField(service, "clock",
                Clock.fixed(START.plusSeconds(seconds), ZoneId.of("Asia/Shanghai")));
    }

    private void failTimes(String username, int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(username, IP, "账号或密码错误");
        }
    }

    private CustomException locked(String username) {
        return assertThrows(CustomException.class, () -> service.checkLocked(username));
    }

    @Test
    @DisplayName("连续错 4 次还能登录，第 5 次锁定；提示剩余分钟数，快到期时是 1 分钟")
    void locksAfterFiveFailures() {
        failTimes("zhangsan", 4);
        assertDoesNotThrow(() -> service.checkLocked("zhangsan"));

        failTimes("zhangsan", 1);
        CustomException e = locked("zhangsan");
        assertEquals("4008", e.getCode());
        assertTrue(e.getMsg().contains("约 10 分钟"), e.getMsg());

        at(9 * 60 + 30);
        assertTrue(locked("zhangsan").getMsg().contains("约 1 分钟"));
    }

    @Test
    @DisplayName("锁 10 分钟后自动解锁，计数从头算：再错一次不会马上又锁")
    void unlocksAfterTenMinutes() {
        failTimes("zhangsan", 5);
        at(10 * 60);
        assertDoesNotThrow(() -> service.checkLocked("zhangsan"));

        failTimes("zhangsan", 1);
        assertDoesNotThrow(() -> service.checkLocked("zhangsan"));
        failTimes("zhangsan", 3);
        assertDoesNotThrow(() -> service.checkLocked("zhangsan"));
        failTimes("zhangsan", 1);
        locked("zhangsan");
    }

    @Test
    @DisplayName("锁定到期后没先查锁就又输错：同样从头计数")
    void expiredLockResetsEvenWithoutCheck() {
        failTimes("zhangsan", 5);
        at(11 * 60);
        failTimes("zhangsan", 1);
        assertDoesNotThrow(() -> service.checkLocked("zhangsan"));
    }

    @Test
    @DisplayName("登录成功清零：错 4 次、成功一次、再错 4 次也不锁")
    void successResetsCount() {
        failTimes("zhangsan", 4);
        service.recordSuccess("zhangsan", IP);
        failTimes("zhangsan", 4);
        assertDoesNotThrow(() -> service.checkLocked("zhangsan"));
    }

    @Test
    @DisplayName("15 分钟内凑不够 5 次就重新计数：零零星星输错不会被锁")
    void failuresExpireAfterWindow() {
        failTimes("zhangsan", 4);
        at(16 * 60);
        failTimes("zhangsan", 1);
        assertDoesNotThrow(() -> service.checkLocked("zhangsan"));

        // 新一轮从第 16 分钟算起：再错 4 次（共 5 次）就锁
        at(20 * 60);
        failTimes("zhangsan", 4);
        locked("zhangsan");
    }

    @Test
    @DisplayName("验证码错误只记日志、不计数：防止别人用错误验证码把账号锁死")
    void captchaFailuresDoNotLock() {
        for (int i = 0; i < 6; i++) {
            service.recordCaptchaFailure("zhangsan", IP);
        }
        assertDoesNotThrow(() -> service.checkLocked("zhangsan"));

        ArgumentCaptor<LoginLog> logs = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogMapper, times(6)).insert(logs.capture());
        assertEquals("验证码错误", logs.getValue().getMsg());
        assertEquals("失败", logs.getValue().getStatus());
    }

    @Test
    @DisplayName("不同账号分开计数：张三被锁不影响李四")
    void accountsAreIndependent() {
        failTimes("zhangsan", 5);
        locked("zhangsan");
        assertDoesNotThrow(() -> service.checkLocked("lisi"));
    }

    @Test
    @DisplayName("登录日志写不进去不影响登录流程，失败照样计数")
    void logFailureDoesNotBreakLogin() {
        doThrow(new RuntimeException("db down")).when(loginLogMapper).insert(any());

        assertDoesNotThrow(() -> service.recordSuccess("lisi", IP));
        assertDoesNotThrow(() -> failTimes("zhangsan", 5));
        locked("zhangsan");
    }
}
