package com.example.service;

import com.example.entity.Message;
import com.example.mapper.MessageMapper;
import com.example.support.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 站内消息：落库字段、超长截断、落库失败不影响主流程、只能操作自己的消息
 */
class MessageServiceTest {

    private final MessageMapper mapper = mock(MessageMapper.class);
    private final MessageService service = new MessageService();

    {
        ReflectionTestUtils.setField(service, "messageMapper", mapper);
        ReflectionTestUtils.setField(service, "clock",
                Clock.fixed(Instant.parse("2026-10-07T02:40:00Z"), ZoneId.of("Asia/Shanghai")));
    }

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    @Test
    @DisplayName("推送落库：接收人、类型、跳转路径与业务时区的时间都写对")
    void pushPersists() {
        service.push(3, "STUDENT", "score", "成绩发布通知", "高等数学成绩已发布", "/course/1/grades");
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mapper).insert(captor.capture());
        Message m = captor.getValue();
        assertEquals(3, m.getUserId());
        assertEquals("STUDENT", m.getRole());
        assertEquals("score", m.getType());
        assertEquals("/course/1/grades", m.getLink());
        assertEquals("2026-10-07 10:40", m.getCreateTime());
    }

    @Test
    @DisplayName("超长标题与内容截断到表结构允许的长度，不让插入报错")
    void truncatesLongText() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 700; i++) {
            longText.append('字');
        }
        service.push(1, "TEACHER", "homework", longText.toString(), longText.toString(), null);
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mapper).insert(captor.capture());
        assertTrue(captor.getValue().getTitle().length() <= 255);
        assertTrue(captor.getValue().getContent().length() <= 500);
    }

    @Test
    @DisplayName("落库失败只记日志：不能因为通知失败让成绩录入等主操作失败")
    void persistFailureIsSwallowed() {
        when(mapper.insert(any())).thenThrow(new RuntimeException("table message doesn't exist"));
        assertDoesNotThrow(() -> service.push(1, "STUDENT", "apply", "请假审核结果", "通过", null));
    }

    @Test
    @DisplayName("没有接收人时什么也不做")
    void ignoresMissingReceiver() {
        service.push(null, "STUDENT", "score", "t", "c", null);
        service.push(1, " ", "score", "t", "c", null);
        verify(mapper, never()).insert(any());
    }

    @Test
    @DisplayName("标记已读、删除都带当前登录人条件，碰不到别人的消息")
    void operationsScopedToCurrentUser() {
        CurrentUser.as("STUDENT", 7, "张三");
        service.markRead(99);
        service.delete(99);
        service.markAllRead();
        verify(mapper).markRead(99, 7, "STUDENT");
        verify(mapper).deleteOwn(99, 7, "STUDENT");
        verify(mapper).markAllRead(7, "STUDENT");
    }

    @Test
    @DisplayName("未登录调用消息接口：报 401")
    void requiresLogin() {
        CurrentUser.clear();
        boolean threw = false;
        try {
            service.unreadCount();
        } catch (com.example.exception.CustomException e) {
            threw = "401".equals(e.getCode());
        }
        assertTrue(threw);
        assertFalse(mockCalledCount());
    }

    private boolean mockCalledCount() {
        return org.mockito.Mockito.mockingDetails(mapper).getInvocations().stream()
                .anyMatch(i -> i.getMethod().getName().equals("countUnread"));
    }
}
