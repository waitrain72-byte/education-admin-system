package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.exception.CustomException;
import com.example.mapper.OrgMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 组织架构树的拼装与「下面还有东西不许删」的检查。
 */
@ExtendWith(MockitoExtension.class)
class OrgServiceTest {

    @Mock
    private OrgMapper orgMapper;

    @InjectMocks
    private OrgService service;

    @Test
    @DisplayName("学院 → 专业 → 班级拼成树，各级带学生人数；没有下级的节点给空列表")
    @SuppressWarnings("unchecked")
    void buildsTreeWithCounts() {
        when(orgMapper.colleges()).thenReturn(Arrays.asList(row("id", 3, "name", "计算机学院"), row("id", 4, "name", "外国语学院")));
        when(orgMapper.specialities()).thenReturn(Arrays.asList(
                row("id", 6, "name", "计算机科学与技术", "collegeId", 3),
                row("id", 1, "name", "物联网工程", "collegeId", 3)));
        when(orgMapper.classes()).thenReturn(Arrays.asList(
                row("id", 5, "name", "计科1班", "specialityId", 6),
                row("id", 2, "name", "物联网1班", "specialityId", 1)));
        when(orgMapper.studentsPerClass()).thenReturn(Arrays.asList(row("classId", 5, "total", 9L), row("classId", 2, "total", 4L)));
        when(orgMapper.studentsPerSpeciality()).thenReturn(Arrays.asList(row("specialityId", 6, "total", 10L), row("specialityId", 1, "total", 4L)));
        when(orgMapper.studentsPerCollege()).thenReturn(Collections.singletonList(row("collegeId", 3, "total", 15L)));

        List<Map<String, Object>> tree = service.tree();

        assertEquals(2, tree.size());
        Map<String, Object> cs = tree.get(0);
        assertEquals("college", cs.get("type"));
        assertEquals(15L, cs.get("students"));
        List<Map<String, Object>> specialities = (List<Map<String, Object>>) cs.get("specialities");
        assertEquals(2, specialities.size());
        assertEquals(10L, specialities.get(0).get("students"));
        List<Map<String, Object>> classes = (List<Map<String, Object>>) specialities.get(0).get("classes");
        assertEquals("计科1班", classes.get(0).get("name"));
        assertEquals(9L, classes.get(0).get("students"));
        // 外国语学院没有专业、没有学生
        assertEquals(0L, tree.get(1).get("students"));
        assertEquals(0, ((List<?>) tree.get(1).get("specialities")).size());
    }

    @Test
    @DisplayName("学院下面还有专业或学生：不许删")
    void collegeWithChildrenCannotBeDeleted() {
        when(orgMapper.countSpecialitiesOfCollege(3)).thenReturn(2);
        assertEquals(ResultCodeEnum.ORG_NOT_EMPTY_ERROR.code,
                assertThrows(CustomException.class, () -> service.requireCollegeEmpty(3)).getCode());

        when(orgMapper.countSpecialitiesOfCollege(4)).thenReturn(0);
        when(orgMapper.countStudentsOfCollege(4)).thenReturn(1);
        assertThrows(CustomException.class, () -> service.requireCollegeEmpty(4));

        when(orgMapper.countSpecialitiesOfCollege(5)).thenReturn(0);
        when(orgMapper.countStudentsOfCollege(5)).thenReturn(0);
        assertDoesNotThrow(() -> service.requireCollegeEmpty(5));
    }

    @Test
    @DisplayName("专业下面还有班级或学生、班级里还有学生：不许删")
    void specialityAndClassGuards() {
        when(orgMapper.countClassesOfSpeciality(6)).thenReturn(1);
        assertThrows(CustomException.class, () -> service.requireSpecialityEmpty(6));

        when(orgMapper.countStudentsOfClass(5)).thenReturn(9);
        assertThrows(CustomException.class, () -> service.requireClassEmpty(5));

        when(orgMapper.countStudentsOfClass(7)).thenReturn(0);
        assertDoesNotThrow(() -> service.requireClassEmpty(7));
    }

    private static Map<String, Object> row(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
