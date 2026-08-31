package com.example.service;

import cn.hutool.core.date.DateUtil;
import com.example.entity.Account;
import com.example.entity.Notice;
import com.example.mapper.CrudMapper;
import com.example.mapper.NoticeMapper;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 公告信息表业务处理（通用增删改查见 {@link CrudService}）
 */
@Service
public class NoticeService extends CrudService<Notice> {

    @Resource
    private NoticeMapper noticeMapper;

    @Override
    protected CrudMapper<Notice> getMapper() {
        return noticeMapper;
    }

    /**
     * 新增：记录发布时间与发布人
     */
    @Override
    public void add(Notice notice) {
        notice.setTime(DateUtil.today());
        Account currentUser = TokenUtils.getCurrentUser();
        notice.setUser(currentUser.getUsername());
        noticeMapper.insert(notice);
    }
}
