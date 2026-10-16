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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 考试安排 Mapper XML：不连数据库，直接让 MyBatis 解析 XML 并生成 SQL，
 * 检查 exam_time、course_id 两列在查询 / 条件 / 新增 / 修改各处都写对了（动态 SQL 的逗号、参数顺序），
 * 以及按学生 / 教师收窄的可见范围。
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

    private static Map<String, Object> between(String from, String to, Integer studentId, Integer teacherId, Integer limit) {
        Map<String, Object> p = new HashMap<>();
        p.put("from", from);
        p.put("to", to);
        p.put("scopeStudentId", studentId);
        p.put("scopeTeacherId", teacherId);
        p.put("limit", limit);
        return p;
    }

    @Test
    @DisplayName("查询带上所属课程与课程名；按考试时间、所属课程过滤时带上条件；管理员不加可见范围")
    void selectAll() {
        BoundSql all = boundSql("selectAll", new Examplan());
        assertTrue(sqlOf(all).startsWith("select e.id,e.name,e.content,e.time,e.exam_time,e.course_id,c.name as course_name"
                + " from examplan e left join course c on c.id = e.course_id"), sqlOf(all));
        assertFalse(sqlOf(all).contains("where"), sqlOf(all));

        Examplan probe = new Examplan();
        probe.setExamTime("2026-12-20 09:00");
        probe.setCourseId(7);
        BoundSql filtered = boundSql("selectAll", probe);
        assertTrue(sqlOf(filtered).contains("e.exam_time= ?"), sqlOf(filtered));
        assertTrue(sqlOf(filtered).contains("e.course_id= ?"), sqlOf(filtered));
        assertEquals(Arrays.asList("examTime", "courseId"), paramsOf(filtered));
    }

    @Test
    @DisplayName("可见范围：学生 = 全校考试 + 自己选的课，教师 = 全校考试 + 自己教的课")
    void selectAllScoped() {
        Examplan student = new Examplan();
        student.setScopeStudentId(3);
        BoundSql s = boundSql("selectAll", student);
        assertTrue(sqlOf(s).contains("(e.course_id is null or e.course_id in"
                + " (select ch.course_id from choice ch where ch.student_id = ?))"), sqlOf(s));
        assertEquals(Collections.singletonList("scopeStudentId"), paramsOf(s));

        Examplan teacher = new Examplan();
        teacher.setScopeTeacherId(2);
        BoundSql t = boundSql("selectAll", teacher);
        assertTrue(sqlOf(t).contains("(e.course_id is null or e.course_id in"
                + " (select co.id from course co where co.teacher_id = ?))"), sqlOf(t));
        assertEquals(Collections.singletonList("scopeTeacherId"), paramsOf(t));
    }

    @Test
    @DisplayName("按时间段查：from 必填，to、范围、条数都可省；按考试时间先后")
    void selectBetween() {
        BoundSql open = boundSql("selectBetween", between("2026-10-15 08:00", null, null, null, null));
        assertTrue(sqlOf(open).contains("WHERE e.exam_time >= ? order by e.exam_time, e.id"), sqlOf(open));
        assertFalse(sqlOf(open).contains("limit"), sqlOf(open));
        assertEquals(Collections.singletonList("from"), paramsOf(open));

        BoundSql week = boundSql("selectBetween", between("2026-10-05", "2026-10-12", 1, null, 3));
        assertTrue(sqlOf(week).contains("e.exam_time >= ? and e.exam_time < ?"), sqlOf(week));
        assertTrue(sqlOf(week).contains("ch.student_id = ?"), sqlOf(week));
        assertTrue(sqlOf(week).endsWith("limit ?"), sqlOf(week));
        assertEquals(Arrays.asList("from", "to", "scopeStudentId", "limit"), paramsOf(week));
    }

    @Test
    @DisplayName("新增：写入 exam_time 与 course_id，列与参数一一对应；全校考试不写 course_id")
    void insert() {
        Examplan e = new Examplan();
        e.setName("期末考试");
        e.setContent("闭卷");
        e.setTime("2026-09-25 10:00:00");
        e.setExamTime("2026-12-20 09:00");
        e.setCourseId(7);
        BoundSql b = boundSql("insert", e);
        assertTrue(sqlOf(b).contains("( name, content, time, exam_time, course_id )"), sqlOf(b));
        assertTrue(sqlOf(b).contains("values ( ?, ?, ?, ?, ? )"), sqlOf(b));
        assertEquals(Arrays.asList("name", "content", "time", "examTime", "courseId"), paramsOf(b));

        e.setCourseId(null);
        assertFalse(sqlOf(boundSql("insert", e)).contains("course_id"));
    }

    @Test
    @DisplayName("修改：未传考试时间时不会被清空；所属课程每次都写，传空就改回全校考试")
    void updateById() {
        Examplan onlyExamTime = new Examplan();
        onlyExamTime.setId(3);
        onlyExamTime.setExamTime("2026-12-20 09:00");
        BoundSql b = boundSql("updateById", onlyExamTime);
        assertEquals("update examplan SET exam_time = ?, course_id = ? where id = ?", sqlOf(b));
        assertEquals(Arrays.asList("examTime", "courseId", "id"), paramsOf(b));

        Examplan withoutExamTime = new Examplan();
        withoutExamTime.setId(3);
        withoutExamTime.setName("期末考试");
        BoundSql b2 = boundSql("updateById", withoutExamTime);
        assertFalse(sqlOf(b2).contains("exam_time"), sqlOf(b2));
        assertTrue(sqlOf(b2).contains("course_id = ?"), sqlOf(b2));
    }
}
