package com.example.controller;

import com.example.common.annotation.RequirePermission;
import com.example.entity.Roomplan;
import com.example.service.CrudService;
import com.example.service.RoomplanService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 教室安排表前端操作接口（通用增删改查见 {@link CrudController}）
 **/
@RestController
@RequestMapping("/roomplan")
@RequirePermission(module = "roomplan")
public class RoomplanController extends CrudController<Roomplan> {

    @Resource
    private RoomplanService roomplanService;

    @Override
    protected CrudService<Roomplan> getService() {
        return roomplanService;
    }
}
