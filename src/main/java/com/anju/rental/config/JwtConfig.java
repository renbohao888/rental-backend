package com.anju.rental.config;

import com.anju.rental.common.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * JWT 配置初始化类
 * <p>
 * 在 Spring 容器启动时自动读取配置文件中的 jwt.secret 和 jwt.expiration，
 * 并注入到 JwtUtils 工具类中，替代硬编码的密钥。
 * </p>
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    @PostConstruct
    public void init() {
        JwtUtils.setConfig(secret, expiration);
    }
}