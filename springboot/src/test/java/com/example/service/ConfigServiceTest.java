package com.example.service;

import com.example.entity.SemesterConfig;
import com.example.exception.CustomException;
import com.example.mapper.ConfigMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学期设置：第几教学周的推算与参数校验
 */
class ConfigServiceTest {

    private final ConfigMapper mapper = mock(ConfigMapper.class);

    /** 业务时钟固定在 2026-10-07（星期三）北京时间上午 */
    private ConfigService serviceAt(String isoInstant) {
        ConfigService service = new ConfigService();
        ReflectionTestUtils.setField(service, "configMapper", mapper);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(Instant.parse(isoInstant), ZoneId.of("Asia/Shanghai")));
        return service;
    }

    @Test
    @DisplayName("教学周：开学当天第 1 周，满 7 天进入第 2 周，开学前为 0")
    void currentWeek() {
        LocalDate start = LocalDate.of(2026, 8, 31);
        assertEquals(0, ConfigService.currentWeek(start, LocalDate.of(2026, 8, 30)));
        assertEquals(1, ConfigService.currentWeek(start, start));
        assertEquals(1, ConfigService.currentWeek(start, LocalDate.of(2026, 9, 6)));
        assertEquals(2, ConfigService.currentWeek(start, LocalDate.of(2026, 9, 7)));
        assertEquals(6, ConfigService.currentWeek(start, LocalDate.of(2026, 10, 7)));
    }

    @Test
    @DisplayName("学期信息按业务时区算「今天」：UTC 前一天 23 点在北京已是次日")
    void semesterUsesBusinessTimezone() {
        when(mapper.selectValue("semester_name")).thenReturn("2026-2027 学年第一学期");
        when(mapper.selectValue("semester_start")).thenReturn("2026-08-31");
        when(mapper.selectValue("semester_weeks")).thenReturn("18");
        // UTC 2026-10-06T23:30 = 北京 10-07 07:30（星期三）
        Map<String, Object> data = serviceAt("2026-10-06T23:30:00Z").semester();
        assertEquals("2026-10-07", data.get("today"));
        assertEquals("星期三", data.get("weekday"));
        assertEquals(6, data.get("currentWeek"));
        assertEquals(18, data.get("weeks"));
    }

    @Test
    @DisplayName("开学日期没配或格式不对：currentWeek 为 null，周数缺省 18，不报错")
    void tolerantOfBadConfig() {
        when(mapper.selectValue(anyString())).thenReturn(null);
        when(mapper.selectValue("semester_start")).thenReturn("not-a-date");
        when(mapper.selectValue("semester_weeks")).thenReturn("abc");
        Map<String, Object> data = serviceAt("2026-10-07T02:00:00Z").semester();
        assertNull(data.get("currentWeek"));
        assertEquals(18, data.get("weeks"));
        assertEquals("", data.get("name"));
    }

    @Test
    @DisplayName("修改学期：名称、日期、周数任一不合法都不落库")
    void rejectsInvalidUpdate() {
        ConfigService service = serviceAt("2026-10-07T02:00:00Z");
        assertThrows(CustomException.class, () -> service.updateSemester(config(" ", "2026-08-31", 18)));
        assertThrows(CustomException.class, () -> service.updateSemester(config("学期", "2026/08/31", 18)));
        assertThrows(CustomException.class, () -> service.updateSemester(config("学期", "2026-08-31", 0)));
        assertThrows(CustomException.class, () -> service.updateSemester(config("学期", "2026-08-31", 31)));
        assertThrows(CustomException.class, () -> service.updateSemester(null));
        verify(mapper, never()).upsert(anyString(), anyString());
    }

    @Test
    @DisplayName("修改学期：合法时三项一起写入，名称去掉首尾空白")
    void savesValidUpdate() {
        serviceAt("2026-10-07T02:00:00Z").updateSemester(config("  2026-2027 学年第一学期 ", "2026-08-31", 20));
        verify(mapper).upsert("semester_name", "2026-2027 学年第一学期");
        verify(mapper).upsert("semester_start", "2026-08-31");
        verify(mapper).upsert("semester_weeks", "20");
    }

    @Test
    @DisplayName("星期名与课程表 week 字段的中文值一致")
    void weekdayNames() {
        assertEquals("星期一", ConfigService.weekdayName(DayOfWeek.MONDAY));
        assertEquals("星期日", ConfigService.weekdayName(DayOfWeek.SUNDAY));
    }

    private SemesterConfig config(String name, String start, Integer weeks) {
        SemesterConfig c = new SemesterConfig();
        c.setName(name);
        c.setStartDate(start);
        c.setWeeks(weeks);
        return c;
    }
}
