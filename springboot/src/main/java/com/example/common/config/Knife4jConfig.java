package com.example.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 接口文档配置（springdoc-openapi 实现）：启动后访问 http://localhost:9091/doc.html
 * 文档数据端点为 /v3/api-docs（已在 WebConfig 中放行 JwtInterceptor）。
 */
@Configuration
public class Knife4jConfig {

    /** 仅扫描 com.example.controller 包下的接口，与业务分组保持一致 */
    @Bean
    public GroupedOpenApi defaultApi() {
        return GroupedOpenApi.builder()
                .group("default")
                .packagesToScan("com.example.controller")
                .build();
    }

    @Bean
    public OpenAPI openApiInfo() {
        return new OpenAPI().info(new Info()
                .title("教务管理系统接口文档")
                .description("前后端分离教务管理系统 RESTful API（管理员 / 教师 / 学生三种角色）")
                .version("1.0"));
    }
}
