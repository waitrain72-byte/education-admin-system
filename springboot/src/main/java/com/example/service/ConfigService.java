package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.WeekEnum;
import com.example.entity.SemesterConfig;
import com.example.exception.CustomException;
import com.example.mapper.ConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 学期参数：学期名称、开学日期、教学周数，以及据此推算的「今天是第几教学周」。
 */
@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    static final String KEY_NAME = "semester_name";
    static final String KEY_START = "semester_start";
    static final String KEY_WEEKS = "semester_weeks";
    static final int DEFAULT_WEEKS = 18;
    static final int MAX_WEEKS = 30;
    static final int MAX_NAME_LENGTH = 50;

    @Resource
    private ConfigMapper configMapper;
    @Resource
    private Clock clock;

    /**
     * 当前学期信息：name / startDate / weeks / currentWeek / today / weekday。
     * currentWeek：开学前为 0，超过教学周数时照常递增（前端据此显示「已结课」）；开学日期没配好时为 null。
     */
    public Map<String, Object> semester() {
        LocalDate today = LocalDate.now(clock);
        String name = value(KEY_NAME);
        LocalDate start = parseDate(value(KEY_START));
        int weeks = parseWeeks(value(KEY_WEEKS));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", name == null ? "" : name);
        data.put("startDate", start == null ? "" : start.toString());
        data.put("weeks", weeks);
        data.put("currentWeek", start == null ? null : currentWeek(start, today));
        data.put("today", today.toString());
        data.put("weekday", weekdayName(today.getDayOfWeek()));
        return data;
    }

    /** 管理员修改学期设置（三项一起校验，任一不合法都不落库） */
    @Transactional(rollbackFor = Exception.class)
    public void updateSemester(SemesterConfig config) {
        if (config == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        String name = StrUtil.trim(config.getName());
        if (StrUtil.isBlank(name) || name.length() > MAX_NAME_LENGTH) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        LocalDate start = parseDate(config.getStartDate());
        if (start == null) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        Integer weeks = config.getWeeks();
        if (weeks == null || weeks < 1 || weeks > MAX_WEEKS) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        configMapper.upsert(KEY_NAME, name);
        configMapper.upsert(KEY_START, start.toString());
        configMapper.upsert(KEY_WEEKS, String.valueOf(weeks));
    }

    /** 今天（业务时区） */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** 第几教学周：开学当天所在周为第 1 周，开学前返回 0 */
    static int currentWeek(LocalDate start, LocalDate today) {
        if (today.isBefore(start)) {
            return 0;
        }
        return (int) (ChronoUnit.DAYS.between(start, today) / 7) + 1;
    }

    /** DayOfWeek → 「星期三」（与课程表 week 字段的中文值一致） */
    public static String weekdayName(DayOfWeek day) {
        return WeekEnum.values()[day.getValue() - 1].week;
    }

    static LocalDate parseDate(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static int parseWeeks(String text) {
        try {
            int weeks = Integer.parseInt(StrUtil.trim(text));
            return weeks >= 1 && weeks <= MAX_WEEKS ? weeks : DEFAULT_WEEKS;
        } catch (NumberFormatException e) {
            return DEFAULT_WEEKS;
        }
    }

    /** 读参数；参数表缺失（升级失败）时按未配置处理，不让首页整个报错 */
    private String value(String key) {
        try {
            return configMapper.selectValue(key);
        } catch (Exception e) {
            log.warn("读取系统参数 {} 失败：{}", key, e.getMessage());
            return null;
        }
    }
}
