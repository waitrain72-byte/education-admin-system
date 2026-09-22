package com.example.service;

import com.example.mapper.DashboardMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 数据大屏聚合统计业务处理。
 *
 * <p>大屏一次要 11 条聚合查询，而前端是定时轮询刷新的，每次都实时重算纯属浪费。
 * 这里加一层 {@link #CACHE_MILLIS} 毫秒的进程内缓存：过期才真正查库，
 * 窗口内的并发请求共享同一份结果。缓存粒度是「整个大屏」，不区分用户——
 * 大屏数据本身是全局统计，不含任何按角色隔离的内容。</p>
 *
 * <p>注：单实例部署适用。多实例部署需换成 Redis 等外置缓存。</p>
 */
@Service
public class DashboardService {

    /** 缓存有效期：大屏由前端定时轮询，30 秒的新鲜度足够 */
    private static final long CACHE_MILLIS = 30 * 1000L;

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter KEY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 登录趋势展示天数 */
    private static final int TREND_DAYS = 7;

    @Resource
    private DashboardMapper dashboardMapper;

    private final AtomicReference<Cached> cache = new AtomicReference<>();

    private static class Cached {
        final Map<String, Object> data;
        final long expireAt;

        Cached(Map<String, Object> data, long expireAt) {
            this.data = data;
            this.expireAt = expireAt;
        }
    }

    /**
     * 大屏全部指标（命中缓存时不查库）。
     * 返回的是不可变视图，避免调用方误改缓存内容。
     */
    public Map<String, Object> stats() {
        Cached cached = cache.get();
        long now = System.currentTimeMillis();
        if (cached != null && cached.expireAt > now) {
            return cached.data;
        }
        Map<String, Object> data = Collections.unmodifiableMap(loadStats());
        cache.set(new Cached(data, now + CACHE_MILLIS));
        return data;
    }

    /** 手动失效缓存（预留给「数据变更后立即刷新大屏」的场景） */
    public void evict() {
        cache.set(null);
    }

    private Map<String, Object> loadStats() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 近 7 天登录趋势先查：今日与本周登录数都从它汇总，省掉两条 COUNT 查询
        List<Map<String, Object>> trend = buildLoginTrend(dashboardMapper.loginTrend());

        // 指标卡
        data.put("studentCount", dashboardMapper.countStudent());
        data.put("teacherCount", dashboardMapper.countTeacher());
        data.put("courseCount", dashboardMapper.countCourse());
        data.put("choiceCount", dashboardMapper.countChoice());
        data.put("loginToday", lastValueOf(trend));
        data.put("loginWeek", sumOf(trend));
        data.put("pendingApply", dashboardMapper.countPendingApply());
        data.put("ungradedHomework", dashboardMapper.countUngradedHomework());

        // 分布图
        data.put("collegeDist", dashboardMapper.collegeDist());
        data.put("courseTop", dashboardMapper.courseTop());
        data.put("titleDist", dashboardMapper.titleDist());

        data.put("loginTrend", trend);
        return data;
    }

    /** 按日期补零，保证返回连续 7 天（趋势图不能因为某天没人登录就断点） */
    private List<Map<String, Object>> buildLoginTrend(List<Map<String, Object>> rows) {
        Map<String, Long> countByDay = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object date = row.get("date");
            Object value = row.get("value");
            countByDay.put(date == null ? "" : String.valueOf(date),
                    value instanceof Number ? ((Number) value).longValue() : 0L);
        }
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> trend = new ArrayList<>(TREND_DAYS);
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", day.format(DAY_FORMAT));
            point.put("value", countByDay.getOrDefault(day.format(KEY_FORMAT), 0L));
            trend.add(point);
        }
        return trend;
    }

    /** 今日登录数：趋势的最后一个点就是今天 */
    private long lastValueOf(List<Map<String, Object>> trend) {
        if (trend.isEmpty()) {
            return 0L;
        }
        return (long) trend.get(trend.size() - 1).get("value");
    }

    /** 近 7 天登录数：趋势各点之和 */
    private long sumOf(List<Map<String, Object>> trend) {
        long sum = 0L;
        for (Map<String, Object> point : trend) {
            sum += (long) point.get("value");
        }
        return sum;
    }
}
