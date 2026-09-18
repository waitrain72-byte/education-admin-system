package com.example.mapper;

import com.example.entity.Roomplan;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作 roomplan 相关数据接口（通用增删改查见 {@link CrudMapper}）
 */
public interface RoomplanMapper extends CrudMapper<Roomplan> {

    /**
     * 空闲教室查询：可排课类型（授课教室/运动场馆）中，未被非结课课程
     * 占用「week + segment」时段、容纳人数 >= minNum 的教室，按容量贴近度排序。
     *
     * @param week      星期（如 星期五）
     * @param segment   大节（与课程表存储值一致）
     * @param minNum    课程上课人数下限（可为 null 表示不限）
     * @param keyword   编号/名称模糊搜索（可为 null）
     * @param typeFilter 类型过滤（如 运动场馆；可为 null 表示全部可排类型）
     * @param excludeId 排除的课程 id（编辑课程时排除自身占用；可为 null）
     */
    List<Roomplan> selectFreeRooms(@Param("week") String week,
                                   @Param("segment") String segment,
                                   @Param("minNum") Integer minNum,
                                   @Param("keyword") String keyword,
                                   @Param("typeFilter") String typeFilter,
                                   @Param("excludeId") Integer excludeId);

    /**
     * 按编号精确查询教室（排除 excludeId 自身），用于保存前编号重复校验
     */
    Roomplan selectByCode(@Param("code") String code, @Param("excludeId") Integer excludeId);
}
