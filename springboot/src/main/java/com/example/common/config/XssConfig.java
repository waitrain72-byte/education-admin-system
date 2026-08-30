package com.example.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * XSS 防御：自定义 Jackson 字符串反序列化器，
 * 对所有 @RequestBody JSON 中的字符串字段做脚本标签与 javascript: 协议中和，
 * 恶意内容入库前即被无害化（Vue 模板插值本身也会转义，形成双重防线）。
 */
@Configuration
public class XssConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer xssStringCustomizer() {
        return builder -> builder.deserializerByType(String.class, new XssStringJsonDeserializer());
    }

    /**
     * 中和处理：仅中和明确的攻击特征（script 标签 / javascript 协议），
     * 不转义全部 HTML，避免影响正常业务文本（如含 & 或 < 的描述）。
     */
    public static class XssStringJsonDeserializer extends StringDeserializer {

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String value = super.deserialize(parser, context);
            if (value == null) {
                return null;
            }
            return value
                    .replaceAll("(?i)<\\s*script", "&lt;script")
                    .replaceAll("(?i)<\\s*/\\s*script", "&lt;/script")
                    .replaceAll("(?i)javascript\\s*:", "");
        }
    }
}
