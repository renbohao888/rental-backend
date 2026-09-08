package com.anju.rental.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 工具类 - 生成和解析 JWT Token
 * <p>
 * 密钥和过期时间从 application.properties 中读取（jwt.secret / jwt.expiration），
 * 未配置时使用默认值兜底。
 * </p>
 */
public class JwtUtils {

    /** 默认密钥（环境变量未配置时兜底，生产环境务必通过配置注入） */
    private static final String DEFAULT_SECRET = "room-rent-system-secret-key-2025-123456";

    /** 默认 Token 有效期：24 小时（毫秒） */
    private static final long DEFAULT_EXPIRE_TIME = 24 * 60 * 60 * 1000;

    /** 密钥和过期时间，通过静态方法 setConfig 初始化 */
    private static Key key;
    private static long expireTime = DEFAULT_EXPIRE_TIME;

    /**
     * 由 Spring 启动时调用，注入配置文件中的 JWT 参数。
     * <p>
     * 调用时机：在任意一个 @Configuration 类或 @Component 中通过 @Value 读取配置后调用。
     * </p>
     */
    public static void setConfig(String secret, long expiration) {
        String actualSecret = (secret != null && !secret.isBlank()) ? secret : DEFAULT_SECRET;
        key = Keys.hmacShaKeyFor(actualSecret.getBytes(StandardCharsets.UTF_8));
        expireTime = expiration > 0 ? expiration : DEFAULT_EXPIRE_TIME;
    }

    private static Key getKey() {
        if (key == null) {
            key = Keys.hmacShaKeyFor(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8));
        }
        return key;
    }

    /**
     * 生成 JWT Token
     *
     * @param userId 用户 ID
     * @param role   角色类型 (0:管理员, 1:房东, 2:租客)
     * @return JWT Token 字符串
     */
    public static String generateToken(Long userId, Integer role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role)
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 JWT Token
     *
     * @param token JWT Token
     * @return Claims 对象，包含 userId 和 role 等信息
     */
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}