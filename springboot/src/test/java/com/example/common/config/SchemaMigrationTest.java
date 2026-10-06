package com.example.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 启动时的增量表结构升级：缺列才补、已有不动、失败不阻止启动。
 */
class SchemaMigrationTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final SchemaMigration migration = new SchemaMigration(jdbc);

    private void columnCount(int count) {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("examplan"), eq("exam_time"))).thenReturn(count);
    }

    @Test
    @DisplayName("老库缺 exam_time 列：执行 ALTER TABLE 补在 time 列之后")
    void addsMissingColumn() {
        columnCount(0);
        migration.run(null);
        verify(jdbc).execute(argThat((String sql) -> sql.startsWith("ALTER TABLE `examplan` ADD COLUMN `exam_time` varchar(20)")
                && sql.endsWith("AFTER `time`")));
    }

    @Test
    @DisplayName("新库已有该列：不执行任何 DDL（幂等）")
    void skipsExistingColumn() {
        columnCount(1);
        assertFalse(migration.addColumnIfMissing("examplan", "exam_time", "varchar(20)"));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    @DisplayName("升级失败（如没有 ALTER 权限）只记日志，不抛出、不阻止启动")
    void failureDoesNotStopStartup() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("examplan"), eq("exam_time")))
                .thenThrow(new RuntimeException("ALTER command denied"));
        assertDoesNotThrow(() -> migration.run(null));
    }

    @Test
    @DisplayName("返回值表示本次是否真的补了列")
    void reportsWhetherColumnWasAdded() {
        columnCount(0);
        assertTrue(migration.addColumnIfMissing("examplan", "exam_time", "varchar(20)"));
    }

    @Test
    @DisplayName("缺表才建：已有的表不执行 DDL，缺的表执行建表语句")
    void createsMissingTableOnly() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("message"))).thenReturn(1);
        assertFalse(migration.createTableIfMissing("message", SchemaMigration.DDL_MESSAGE));
        verify(jdbc, never()).execute(anyString());

        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("sys_config"))).thenReturn(0);
        assertTrue(migration.createTableIfMissing("sys_config", SchemaMigration.DDL_SYS_CONFIG));
        verify(jdbc).execute(SchemaMigration.DDL_SYS_CONFIG);
    }

    @Test
    @DisplayName("权限点已存在：不插入也不改授权（尊重管理员在权限设置页的调整）")
    void existingPermissionUntouched() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("config:manage"))).thenReturn(1);
        assertFalse(migration.ensurePermission("config:manage", "学期设置", "menu", "config", 1, "ADMIN"));
        assertTrue(mockingDetails(jdbc).getInvocations().stream()
                .noneMatch(invocation -> invocation.getMethod().getName().equals("update")));
    }

    @Test
    @DisplayName("新权限点：插入后逐个授予默认角色")
    void newPermissionGrantedToRoles() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("resource:view"))).thenReturn(0);
        assertTrue(migration.ensurePermission("resource:view", "课程资料-查看", "menu", "resource", 1, "TEACHER", "STUDENT"));
        verify(jdbc).update(startsWith("INSERT INTO sys_permission"), eq("resource:view"), eq("课程资料-查看"),
                eq("menu"), eq("resource"), eq(1));
        verify(jdbc).update(startsWith("INSERT INTO sys_role_permission"), eq("resource:view"), eq("TEACHER"));
        verify(jdbc).update(startsWith("INSERT INTO sys_role_permission"), eq("resource:view"), eq("STUDENT"));
    }

    @Test
    @DisplayName("默认学期：秋季按 9 月 1 日所在周开学，1 月仍属上一学年第一学期，春季按 3 月 1 日所在周")
    void defaultSemester() {
        assertArrayEquals(new String[]{"2026-2027 学年第一学期", "2026-08-31"},
                SchemaMigration.defaultSemester(LocalDate.of(2026, 10, 7)));
        assertArrayEquals(new String[]{"2026-2027 学年第一学期", "2026-08-31"},
                SchemaMigration.defaultSemester(LocalDate.of(2027, 1, 10)));
        assertArrayEquals(new String[]{"2026-2027 学年第二学期", "2027-03-01"},
                SchemaMigration.defaultSemester(LocalDate.of(2027, 4, 2)));
    }
}
