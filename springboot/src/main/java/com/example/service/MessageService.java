package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.Message;
import com.example.exception.CustomException;
import com.example.mapper.MessageMapper;
import com.example.utils.TokenUtils;
import com.example.websocket.NoticeWebSocketServer;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 站内消息：业务推送统一走 {@link #push}——先落库再经 WebSocket 实时送达，
 * 不在线的用户下次登录也能在消息中心看到；读取、标记、删除都只针对当前登录人自己的消息。
 */
@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** 与表结构一致的长度上限，超长截断而不是让插入报错 */
    private static final int MAX_TITLE = 255;
    private static final int MAX_CONTENT = 500;
    private static final int MAX_PAGE_SIZE = 50;

    @Resource
    private MessageMapper messageMapper;
    @Resource
    private Clock clock;

    /**
     * 发一条消息给指定用户：落库 + 实时推送。
     *
     * <p>落库失败只记日志、仍然尝试实时推送：消息是业务操作的附带通知，不能因为它让成绩录入、请假审核这些主操作失败。</p>
     */
    public void push(Integer userId, String role, String type, String title, String content, String link) {
        if (userId == null || StrUtil.isBlank(role)) {
            return;
        }
        Message message = new Message();
        message.setUserId(userId);
        message.setRole(role);
        message.setType(type);
        message.setTitle(StrUtil.maxLength(title, MAX_TITLE - 3));
        message.setContent(StrUtil.maxLength(content, MAX_CONTENT - 3));
        message.setLink(link);
        message.setCreateTime(LocalDateTime.now(clock).format(TIME_FORMAT));
        try {
            messageMapper.insert(message);
        } catch (Exception e) {
            log.warn("站内消息落库失败（仍尝试实时推送）：{}", e.getMessage());
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", message.getId());
        payload.put("type", type);
        payload.put("title", message.getTitle());
        payload.put("content", message.getContent());
        payload.put("link", link);
        payload.put("time", message.getCreateTime());
        NoticeWebSocketServer.sendPayload(userId, role, payload);
    }

    /** 当前登录人的消息分页（可只看未读） */
    public PageInfo<Message> page(boolean unreadOnly, Integer pageNum, Integer pageSize) {
        Account current = requireLogin();
        int size = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
        PageHelper.startPage(pageNum == null ? 1 : Math.max(1, pageNum), size);
        List<Message> list = messageMapper.selectByReceiver(current.getId(), current.getRole(), unreadOnly);
        return PageInfo.of(list);
    }

    public int unreadCount() {
        Account current = requireLogin();
        return messageMapper.countUnread(current.getId(), current.getRole());
    }

    public void markRead(Integer id) {
        Account current = requireLogin();
        messageMapper.markRead(id, current.getId(), current.getRole());
    }

    public void markAllRead() {
        Account current = requireLogin();
        messageMapper.markAllRead(current.getId(), current.getRole());
    }

    public void delete(Integer id) {
        Account current = requireLogin();
        messageMapper.deleteOwn(id, current.getId(), current.getRole());
    }

    private Account requireLogin() {
        Account current = TokenUtils.getCurrentUser();
        if (current.getId() == null || StrUtil.isBlank(current.getRole())) {
            throw new CustomException(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        return current;
    }
}
