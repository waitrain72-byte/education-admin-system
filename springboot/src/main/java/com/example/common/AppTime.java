package com.example.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 业务时间的静态入口：发布时间、评教时间等写进数据库的「当前时间」统一从这里取。
 *
 * <p>不能用 JVM 默认时区（DateUtil.now() 就是）：Docker 镜像默认 UTC，北京时间零点到八点之间
 * 记下的日期会早一天，和首页「今天」对不上。时钟由 {@link com.example.common.config.ClockConfig}
 * 按 app.timezone 注入；单元测试里没有 Spring 容器时默认用北京时间。</p>
 */
public final class AppTime {

    private static final DateTimeFormatter SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static volatile Clock clock = Clock.system(ZoneId.of("Asia/Shanghai"));

    private AppTime() {
    }

    /** 由 ClockConfig 在启动时调用 */
    public static void use(Clock businessClock) {
        clock = businessClock;
    }

    public static Clock clock() {
        return clock;
    }

    /** yyyy-MM-dd */
    public static String today() {
        return LocalDate.now(clock).toString();
    }

    /** yyyy-MM-dd HH:mm:ss */
    public static String now() {
        return LocalDateTime.now(clock).format(SECONDS);
    }
}
