package com.example.mapper;

import com.example.entity.Examplan;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 考试安排 Mapper XML：不连数据库，直接让 MyBatis 解析 XML 并生成 SQL，
 * 检查新增的 exam_time 列在查询 / 条件 / 新增 / 修改四处都写对了（动态 SQL 的逗号、参数顺序）。
 */
class ExamplanMapperXmlTest {

    private static Configuration configuration;

    @BeforeAll
    static void parseMapperXml() throws Exception {
        configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        String resource = "mapper/ExamplanMapper.xml";
        try (InputStream in = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private static BoundSql boundSql(String id, Object param) {
        return configuration.getMappedStatement("com.example.mapper.ExamplanMapper." + id).getBoundSql(param);
    }

    private static String sqlOf(BoundSql b) {
        return b.getSql().replaceAll("\\s+", " ").trim();
    }

    private static List<String> paramsOf(BoundSql b) {
        return b.getParameterMappings().stream().map(ParameterMapping::getProperty).collect(Collectors.toList());
    }

    @Test
    @DisplayName("查询列包含 exam_time；按考试时间过滤时带上条件")
    void selectAll() {
        BoundSql all = boundSql("selectAll", new Examplan());
        assertTrue(sqlOf(all).startsWith("select id,name,content,time,exam_time from examplan"), sqlOf(all));

        Examplan probe = new Examplan();
        probe.setExamTime("2026-12-20 09:00");
        BoundSql filtered = boundSql("selectAll", probe);
        assertTrue(sqlOf(filtered).contains("exam_time= ?"), sqlOf(filtered));
        assertEquals(Collections.singletonList("examTime"), paramsOf(filtered));
    }

    @Test
    @DisplayName("新增：写入 exam_time，列与参数一一对应")
    void insert() {
        Examplan e = new Examplan();
        e.setName("期末考试");
        e.setContent("闭卷");
        e.setTime("2026-09-25 10:00:00");
        e.setExamTime("2026-12-20 09:00");
        BoundSql b = boundSql("insert", e);
        assertTrue(sqlOf(b).contains("( name, content, time, exam_time )"), sqlOf(b));
        assertTrue(sqlOf(b).contains("values ( ?, ?, ?, ? )"), sqlOf(b));
        assertEquals(Arrays.asList("name", "content", "time", "examTime"), paramsOf(b));
    }

    @Test
    @DisplayName("修改：只改考试时间时 SET 子句只有 exam_time；未传考试时间时不会被清空")
    void updateById() {
        Examplan onlyExamTime = new Examplan();
        onlyExamTime.setId(3);
        onlyExamTime.setExamTime("2026-12-20 09:00");
        BoundSql b = boundSql("updateById", onlyExamTime);
        assertEquals("update examplan SET exam_time = ? where id = ?", sqlOf(b));
        assertEquals(Arrays.asList("examTime", "id"), paramsOf(b));

        Examplan withoutExamTime = new Examplan();
        withoutExamTime.setId(3);
        withoutExamTime.setName("期末考试");
        BoundSql b2 = boundSql("updateById", withoutExamTime);
        assertFalse(sqlOf(b2).contains("exam_time"), sqlOf(b2));
    }
}
