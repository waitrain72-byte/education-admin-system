package com.example.mapper;

import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 组织架构（学院 → 专业 → 班级）的聚合查询：只读。
 */
public interface OrgMapper {

    @Select("select id, name, content from college order by id")
    List<Map<String, Object>> colleges();

    @Select("select id, name, content, college_id as collegeId, score from speciality order by id")
    List<Map<String, Object>> specialities();

    @Select("select c.id, c.name, c.content, c.speciality_id as specialityId, c.teacher_id as teacherId, t.name as teacherName"
            + " from classes c left join teacher t on c.teacher_id = t.id order by c.id")
    List<Map<String, Object>> classes();

    /** 每个班的学生数：classId / total */
    @Select("select class_id as classId, count(*) as total from student where class_id is not null group by class_id")
    List<Map<String, Object>> studentsPerClass();

    /** 每个专业的学生数（含还没分班的）：specialityId / total */
    @Select("select speciality_id as specialityId, count(*) as total from student where speciality_id is not null group by speciality_id")
    List<Map<String, Object>> studentsPerSpeciality();

    /** 每个学院的学生数（含还没分专业的）：collegeId / total */
    @Select("select college_id as collegeId, count(*) as total from student where college_id is not null group by college_id")
    List<Map<String, Object>> studentsPerCollege();

    @Select("select count(*) from speciality where college_id = #{collegeId}")
    int countSpecialitiesOfCollege(Integer collegeId);

    @Select("select count(*) from classes where speciality_id = #{specialityId}")
    int countClassesOfSpeciality(Integer specialityId);

    @Select("select count(*) from student where college_id = #{collegeId}")
    int countStudentsOfCollege(Integer collegeId);

    @Select("select count(*) from student where speciality_id = #{specialityId}")
    int countStudentsOfSpeciality(Integer specialityId);

    @Select("select count(*) from student where class_id = #{classId}")
    int countStudentsOfClass(Integer classId);
}
