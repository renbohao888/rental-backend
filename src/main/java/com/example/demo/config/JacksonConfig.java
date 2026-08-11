package com.example.demo.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全局 Jackson 配置
 * 将 Long 类型字段统一序列化为 JSON 字符串，避免雪花ID（订单、报修、公告等）
 * 超过 JavaScript 安全整数上限 Number.MAX_SAFE_INTEGER（9007199254740991）
 * 而在前端被截断/失真，导致详情跳转、撤销、支付等操作报"不存在/失败"。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToJsonStringCustomizer() {
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance);
    }
}
