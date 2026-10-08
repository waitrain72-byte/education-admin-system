package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Roomplan;
import com.example.exception.CustomException;
import com.example.mapper.CourseMapper;
import com.example.mapper.CrudMapper;
import com.example.mapper.RoomplanMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 教室安排表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class RoomplanService extends CrudService<Roomplan> {

    @Resource
    private RoomplanMapper roomplanMapper;
    @Resource
    private CourseMapper courseMapper;

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
     * 修改教室前校验编号重复（排除自身）；改了编号的话，课程上记的编号一起改，免得课程挂在一个不存在的教室上
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(Roomplan roomplan) {
        if (roomplan == null || roomplan.getId() == null) {
            throw new CustomException(ResultCodeEnum.PARAM_LOST_ERROR);
        }
        checkCodeDuplicate(roomplan);
        Roomplan before = roomplanMapper.selectById(roomplan.getId());
        super.updateById(roomplan);
        if (before != null && StrUtil.isNotBlank(before.getCode()) && StrUtil.isNotBlank(roomplan.getCode())
                && !before.getCode().equals(roomplan.getCode())) {
            courseMapper.renameRoom(before.getCode(), roomplan.getCode());
        }
    }

    /**
     * 删除教室：还有没结课的课排在这里时不许删（先给那些课换教室）
     */
    @Override
    public void deleteById(Integer id) {
        Roomplan room = id == null ? null : roomplanMapper.selectById(id);
        if (room != null && StrUtil.isNotBlank(room.getCode()) && courseMapper.countActiveByRoom(room.getCode()) > 0) {
            throw new CustomException(ResultCodeEnum.ROOM_IN_USE_ERROR);
        }
        super.deleteById(id);
    }

    /**
     * 批量删除：逐条检查
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Integer id : ids) {
            deleteById(id);
        }
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
