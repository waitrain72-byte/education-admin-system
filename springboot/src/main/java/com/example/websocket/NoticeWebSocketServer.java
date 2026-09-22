package com.example.websocket;

import cn.hutool.json.JSONUtil;
import com.example.utils.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
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
        // 同一用户重连时顶掉旧连接：旧 session 必须显式关闭，否则会成为孤儿连接常驻内存
        Session old = SESSIONS.put(userKey, session);
        if (old != null && old != session) {
            closeQuietly(old);
        }
        log.debug("WebSocket 连接建立：{}", userKey);
    }

    @OnClose
    public void onClose() {
        unregister();
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.warn("WebSocket 连接异常：{}", userKey, error);
        unregister();
    }

    /**
     * 注销本连接的登记。
     *
     * <p>必须用「值匹配删除」：每个连接对应一个独立的端点实例，重连时旧实例的 onClose 会在
     * 新实例登记之后才触发，若按 key 无条件 remove 就会把刚注册的新 session 一起删掉，
     * 该用户此后再也收不到任何推送。</p>
     */
    private void unregister() {
        if (userKey != null && session != null) {
            SESSIONS.remove(userKey, session);
        }
    }

    /**
     * 客户端心跳保活：移动端每 25s 发送一次 "ping"，服务端静默忽略、无需回复。
     * 没有该处理器时客户端消息无人接收；有了它连接不会因空闲过久被网关/代理断开。
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.debug("WebSocket 心跳：{} -> {}", userKey, message);
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
            // 必须用 getBasicRemote 同步发送：getAsyncRemote 在方法返回后才真正写出，
            // synchronized 保护不到实际写入，全员广播时会抛 IllegalStateException: TEXT_FULL_WRITING
            synchronized (session) {
                session.getBasicRemote().sendText(JSONUtil.toJsonStr(message));
            }
        } catch (Exception e) {
            log.warn("WebSocket 推送失败: {}", e.getMessage());
        }
    }

    private static void closeQuietly(Session session) {
        try {
            session.close();
        } catch (IOException ignored) {
        }
    }
}
