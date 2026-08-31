package com.example.service;

import com.example.entity.Roomplan;
import com.example.mapper.CrudMapper;
import com.example.mapper.RoomplanMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
}
