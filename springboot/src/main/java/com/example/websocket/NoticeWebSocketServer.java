package com.example.websocket;

import cn.hutool.json.JSONUtil;
import com.example.utils.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时通知 WebSocket 服务端。
 *
 * 连接方式：ws://主机:9091/ws/notice/{token}（token 为登录返回的 JWT）
 * - 连接建立时校验 token，非法连接直接关闭
 * - 连接按 "用户ID-角色" 登记，业务代码可精确推送给某个用户，也可全员广播
 *
 * 推送入口（静态方法，业务代码直接调用）：
 * - sendToUser(userId, role, title, content)：推送给指定用户
 * - sendToAll(title, content)：全员广播
 */
@ServerEndpoint(value = "/ws/notice/{token}")
@Component
public class NoticeWebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(NoticeWebSocketServer.class);

    /** 在线连接：key = 用户ID-角色 */
    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    private Session session;
    private String userKey;

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        // 校验 token（与 JWT 拦截器同一套验签逻辑）
        String payload = TokenUtils.verifyToken(token);
        if (payload == null) {
            closeQuietly(session);
            return;
        }
        String[] parts = payload.split("-");
        if (parts.length != 2) {
            closeQuietly(session);
            return;
        }
        this.userKey = parts[0] + "-" + parts[1];
        this.session = session;
        SESSIONS.put(userKey, session);
        log.debug("WebSocket 连接建立：{}", userKey);
    }

    @OnClose
    public void onClose() {
        if (userKey != null) {
            SESSIONS.remove(userKey);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        if (userKey != null) {
            SESSIONS.remove(userKey);
        }
    }

    /**
     * 推送给指定用户（studentId/teacherId/adminId + 角色）
     */
    public static void sendToUser(Integer userId, String role, String title, String content) {
        Session session = SESSIONS.get(userId + "-" + role);
        if (session != null && session.isOpen()) {
            send(session, title, content);
        }
    }

    /**
     * 全员广播
     */
    public static void sendToAll(String title, String content) {
        for (Session session : SESSIONS.values()) {
            if (session.isOpen()) {
                send(session, title, content);
            }
        }
    }

    private static void send(Session session, String title, String content) {
        try {
            Map<String, String> message = new java.util.HashMap<>();
            message.put("title", title);
            message.put("content", content);
            synchronized (session) {
                session.getAsyncRemote().sendText(JSONUtil.toJsonStr(message));
            }
        } catch (Exception ignored) {
        }
    }

    private static void closeQuietly(Session session) {
        try {
            session.close();
        } catch (IOException ignored) {
        }
    }
}
