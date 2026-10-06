package com.example.common.config;

import com.example.common.AppTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 业务时钟：「今天星期几」「第几教学周」「签到是否过期」都按它计算。
 *
 * <p>不能直接用 JVM 默认时区：Docker 镜像默认是 UTC，北京时间零点到八点之间会把日期算成前一天，
 * 首页的今日课程、签到截止时间都会错。时区可用 app.timezone（环境变量 APP_TIMEZONE）覆盖。</p>
 *
 * <p>以 Bean 注入还有一个好处：单元测试可以传入固定时钟，不依赖运行测试的那一刻。</p>
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock appClock(@Value("${app.timezone:Asia/Shanghai}") String zone) {
        Clock clock = Clock.system(ZoneId.of(zone));
        // 不经 Spring 注入的地方（发布时间等）走 AppTime 静态入口，与这里同一个时钟
        AppTime.use(clock);
        return clock;
    }
}
