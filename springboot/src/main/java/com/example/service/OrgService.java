package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.exception.CustomException;
import com.example.mapper.OrgMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 组织架构：学院 → 专业 → 班级的树，带各级学生人数；以及删除前的「下面还有没有东西」检查。
 *
 * <p>增删改沿用 /college、/speciality、/classes 的通用接口；它们的删除会先调这里的检查，
 * 避免删掉学院后专业、学生挂在一个不存在的学院下面。</p>
 */
@Service
public class OrgService {

    @Resource
    private OrgMapper orgMapper;

    /** 整棵树：[{学院, specialities: [{专业, classes: [{班级}]}]}]，每个节点带 students 人数 */
    public List<Map<String, Object>> tree() {
        Map<Integer, Long> perClass = countMap(orgMapper.studentsPerClass(), "classId");
        Map<Integer, Long> perSpeciality = countMap(orgMapper.studentsPerSpeciality(), "specialityId");
        Map<Integer, Long> perCollege = countMap(orgMapper.studentsPerCollege(), "collegeId");

        Map<Integer, List<Map<String, Object>>> classesBySpeciality = new HashMap<>();
        for (Map<String, Object> c : orgMapper.classes()) {
            Integer id = toInt(c.get("id"));
            Map<String, Object> node = new LinkedHashMap<>(c);
            node.put("type", "class");
            node.put("students", perClass.getOrDefault(id, 0L));
            classesBySpeciality.computeIfAbsent(toInt(c.get("specialityId")), k -> new ArrayList<>()).add(node);
        }

        Map<Integer, List<Map<String, Object>>> specialitiesByCollege = new HashMap<>();
        for (Map<String, Object> s : orgMapper.specialities()) {
            Integer id = toInt(s.get("id"));
            Map<String, Object> node = new LinkedHashMap<>(s);
            node.put("type", "speciality");
            node.put("students", perSpeciality.getOrDefault(id, 0L));
            node.put("classes", classesBySpeciality.getOrDefault(id, new ArrayList<>()));
            specialitiesByCollege.computeIfAbsent(toInt(s.get("collegeId")), k -> new ArrayList<>()).add(node);
        }

        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map<String, Object> c : orgMapper.colleges()) {
            Integer id = toInt(c.get("id"));
            Map<String, Object> node = new LinkedHashMap<>(c);
            node.put("type", "college");
            node.put("students", perCollege.getOrDefault(id, 0L));
            node.put("specialities", specialitiesByCollege.getOrDefault(id, new ArrayList<>()));
            tree.add(node);
        }
        return tree;
    }

    /** 学院下面还有专业或学生时不许删 */
    public void requireCollegeEmpty(Integer collegeId) {
        if (collegeId != null && (orgMapper.countSpecialitiesOfCollege(collegeId) > 0 || orgMapper.countStudentsOfCollege(collegeId) > 0)) {
            throw new CustomException(ResultCodeEnum.ORG_NOT_EMPTY_ERROR);
        }
    }

    /** 专业下面还有班级或学生时不许删 */
    public void requireSpecialityEmpty(Integer specialityId) {
        if (specialityId != null && (orgMapper.countClassesOfSpeciality(specialityId) > 0 || orgMapper.countStudentsOfSpeciality(specialityId) > 0)) {
            throw new CustomException(ResultCodeEnum.ORG_NOT_EMPTY_ERROR);
        }
    }

    /** 班级里还有学生时不许删 */
    public void requireClassEmpty(Integer classId) {
        if (classId != null && orgMapper.countStudentsOfClass(classId) > 0) {
            throw new CustomException(ResultCodeEnum.ORG_NOT_EMPTY_ERROR);
        }
    }

    private static Map<Integer, Long> countMap(List<Map<String, Object>> rows, String key) {
        Map<Integer, Long> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer id = toInt(row.get(key));
            Object total = row.get("total");
            if (id != null && total instanceof Number) {
                map.put(id, ((Number) total).longValue());
            }
        }
        return map;
    }

    private static Integer toInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }
}
