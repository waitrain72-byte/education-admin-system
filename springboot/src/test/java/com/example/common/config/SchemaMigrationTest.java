package com.example.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
}
