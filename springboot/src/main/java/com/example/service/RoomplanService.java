package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Roomplan;
import com.example.exception.CustomException;
import com.example.mapper.CrudMapper;
import com.example.mapper.RoomplanMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 教室安排表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class RoomplanService extends CrudService<Roomplan> {

    @Resource
    private RoomplanMapper roomplanMapper;

    @Override
    protected CrudMapper<Roomplan> getMapper() {
        return roomplanMapper;
    }

    /**
     * 新增教室前校验编号重复
     */
    @Override
    public void add(Roomplan roomplan) {
        checkCodeDuplicate(roomplan);
        super.add(roomplan);
    }

    /**
     * 修改教室前校验编号重复（排除自身）
     */
    @Override
    public void updateById(Roomplan roomplan) {
        if (roomplan == null || roomplan.getId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        checkCodeDuplicate(roomplan);
        super.updateById(roomplan);
    }

    /**
     * 空闲教室查询（供排课表单选择与系统自动分配）：
     * 可排课类型（授课教室/运动场馆）中，未被非结课课程占用「week + segment」时段、
     * 容纳人数 >= minNum 的教室，按容量贴近人数、再按编号排序；typeFilter 可限定运动场馆等。
     */
    public List<Roomplan> selectFreeRooms(String week, String segment, Integer minNum, String keyword, String typeFilter, Integer excludeId) {
        return roomplanMapper.selectFreeRooms(week, segment, minNum, keyword, typeFilter, excludeId);
    }

    /**
     * 编号重复校验：course.room 与 roomplan.code 为精确匹配的排课关联，
     * 编号重复会破坏占用判定；更新时排除自身 id。
     */
    private void checkCodeDuplicate(Roomplan roomplan) {
        if (roomplan == null || StrUtil.isBlank(roomplan.getCode())) {
            return;
        }
        Roomplan exist = roomplanMapper.selectByCode(roomplan.getCode(), roomplan.getId());
        if (exist != null) {
            throw new CustomException(ResultCodeEnum.ROOM_CODE_EXIST_ERROR);
        }
    }
}
