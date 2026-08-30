package com.example.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置：扫描并注册 @ServerEndpoint 服务端点。
 * 注意：使用外部 Servlet 容器部署 war 包时不要注册此 Bean（内嵌 Tomcat 需要注册）。
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
