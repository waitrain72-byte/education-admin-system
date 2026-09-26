package com.example.service;

import com.example.entity.Examplan;
import com.example.exception.CustomException;
import com.example.mapper.ExamplanMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 考试安排：考试时间校验。Web 与小程序据考试时间显示倒计时，格式必须是两端都能解析的 yyyy-MM-dd HH:mm。
 */
class ExamplanServiceTest {

    private ExamplanService service;
    private ExamplanMapper mapper;

    @BeforeEach
    void setUp() {
        service = new ExamplanService();
        mapper = mock(ExamplanMapper.class);
        ReflectionTestUtils.setField(service, "examplanMapper", mapper);
    }

    private Examplan plan(String examTime) {
        Examplan e = new Examplan();
        e.setId(3);
        e.setName("期末考试");
        e.setContent("闭卷");
        e.setExamTime(examTime);
        return e;
    }

    @Test
    @DisplayName("考试时间为空时放行（历史数据），填写了必须是真实存在的 yyyy-MM-dd HH:mm")
    void examTimeFormat() {
        assertTrue(ExamplanService.isValidExamTime(null));
        assertTrue(ExamplanService.isValidExamTime(""));
        assertTrue(ExamplanService.isValidExamTime("2026-12-20 09:00"));
        assertTrue(ExamplanService.isValidExamTime("2028-02-29 23:59"));

        assertFalse(ExamplanService.isValidExamTime("2026-12-20"));
        assertFalse(ExamplanService.isValidExamTime("2026-12-20 09:00:00"));
        assertFalse(ExamplanService.isValidExamTime("2026/12/20 09:00"));
        assertFalse(ExamplanService.isValidExamTime("2026-12-20 9:00"));
        assertFalse(ExamplanService.isValidExamTime("2026-02-30 09:00"));
        assertFalse(ExamplanService.isValidExamTime("2027-02-29 09:00"));
        assertFalse(ExamplanService.isValidExamTime("2026-12-20 24:00"));
        assertFalse(ExamplanService.isValidExamTime("明天上午"));
    }

    @Test
    @DisplayName("新增：考试时间非法时拒绝且不入库；合法时生成发布时间后入库")
    void addValidatesExamTime() {
        assertThrows(CustomException.class, () -> service.add(plan("2026-13-01 09:00")));
        verify(mapper, never()).insert(any());

        Examplan ok = plan("2026-12-20 09:00");
        service.add(ok);
        verify(mapper).insert(ok);
        assertNotNull(ok.getTime());
        assertEquals("2026-12-20 09:00", ok.getExamTime());
    }

    @Test
    @DisplayName("修改：考试时间非法时拒绝；发布时间只在新增时生成，修改时不允许改写")
    void updateValidatesExamTime() {
        assertThrows(CustomException.class, () -> service.updateById(plan("2026-12-20")));
        verify(mapper, never()).updateById(any());

        Examplan ok = plan("2026-12-20 09:00");
        ok.setTime("2020-01-01 00:00:00");
        service.updateById(ok);
        verify(mapper).updateById(ok);
        assertNull(ok.getTime());
        assertEquals("2026-12-20 09:00", ok.getExamTime());
    }
}
