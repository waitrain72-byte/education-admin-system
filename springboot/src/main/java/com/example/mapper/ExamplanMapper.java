package com.example.mapper;

import com.example.entity.Examplan;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作 examplan 相关数据接口（通用增删改查见 {@link CrudMapper}）
 */
public interface ExamplanMapper extends CrudMapper<Examplan> {

    /**
     * 考试时间在 [from, to) 之间的考试，按考试时间先后；没填考试时间的不算。
     *
     * @param to             为空表示不设上限
     * @param scopeStudentId 非空时只查全校考试 + 这名学生选了的课的考试
     * @param scopeTeacherId 非空时只查全校考试 + 这位老师教的课的考试
     * @param limit          为空表示不限条数
     */
    List<Examplan> selectBetween(@Param("from") String from, @Param("to") String to,
                                 @Param("scopeStudentId") Integer scopeStudentId,
                                 @Param("scopeTeacherId") Integer scopeTeacherId,
                                 @Param("limit") Integer limit);

    /** 删除一门课的全部考试（删课时一并清理） */
    int deleteByCourseId(Integer courseId);
}
