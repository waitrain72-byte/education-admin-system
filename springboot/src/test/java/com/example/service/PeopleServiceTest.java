package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.exception.CustomException;
import com.example.mapper.PeopleMapper;
import com.example.support.CurrentUser;
import com.github.pagehelper.PageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 人员页：只有管理员能查；搜索词去空格、截长，空串当作不筛选。
 */
@ExtendWith(MockitoExtension.class)
class PeopleServiceTest {

    @Mock
    private PeopleMapper peopleMapper;

    @InjectMocks
    private PeopleService service;

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
        PageHelper.clearPage();
    }

    @Test
    @DisplayName("老师、学生查不了人员列表")
    void onlyAdminsCanList() {
        CurrentUser.as("TEACHER", 2, "路易斯");

        CustomException e = assertThrows(CustomException.class, () -> service.students(null, null, null, null, 1, 10));
        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, e.getCode());
        verify(peopleMapper, never()).students(any(), any(), any(), any());
    }

    @Test
    @DisplayName("搜索词去掉首尾空格；空白当作不筛选；过长的截到 30 个字")
    void keywordIsNormalized() {
        CurrentUser.as("ADMIN", 1, "管理员");
        when(peopleMapper.teachers("王")).thenReturn(Collections.emptyList());

        service.teachers("  王 ", 1, 10);
        verify(peopleMapper).teachers("王");

        assertNull(PeopleService.normalize("   "));
        assertEquals(30, PeopleService.normalize(repeat('长', 40)).length());
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
