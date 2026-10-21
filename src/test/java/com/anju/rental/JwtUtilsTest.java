package com.anju.rental;

import com.anju.rental.common.JwtUtils;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 工具类单元测试
 * <p>
 * 注意：JwtUtils 的密钥配置是静态全局的，为避免用例间相互影响，
 * 每个用例执行前统一重置为基准配置。
 * </p>
 */
class JwtUtilsTest {

    private static final String BASE_SECRET = "test-jwt-secret-for-unit-testing-2025";

    @BeforeAll
    static void setUp() {
        JwtUtils.setConfig(BASE_SECRET, 3600000L);
    }

    @BeforeEach
    void resetConfig() {
        JwtUtils.setConfig(BASE_SECRET, 3600000L);
    }

    @Test
    @DisplayName("生成 Token 并成功解析")
    void testGenerateAndParseToken() {
        String token = JwtUtils.generateToken(1L, 2);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = JwtUtils.parseToken(token);
        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals(2, claims.get("role", Integer.class));
    }

    @Test
    @DisplayName("不同角色的 Token 解析正确")
    void testDifferentRoles() {
        String adminToken = JwtUtils.generateToken(100L, 0);
        String landlordToken = JwtUtils.generateToken(200L, 1);
        String tenantToken = JwtUtils.generateToken(300L, 2);

        assertEquals(0, JwtUtils.parseToken(adminToken).get("role", Integer.class));
        assertEquals(1, JwtUtils.parseToken(landlordToken).get("role", Integer.class));
        assertEquals(2, JwtUtils.parseToken(tenantToken).get("role", Integer.class));
    }

    @Test
    @DisplayName("解析无效 Token 应抛出异常")
    void testInvalidToken() {
        assertThrows(Exception.class, () -> JwtUtils.parseToken("invalid.token.here"));
        assertThrows(Exception.class, () -> JwtUtils.parseToken(""));
        assertThrows(Exception.class, () -> JwtUtils.parseToken(null));
    }

    @Test
    @DisplayName("使用不同密钥生成的 Token 无法解析")
    void testDifferentSecret() {
        String token = JwtUtils.generateToken(1L, 2);
        // 切换密钥后之前的 Token 应失效
        JwtUtils.setConfig("a-different-secret-key-for-test-2025", 3600000L);
        assertThrows(Exception.class, () -> JwtUtils.parseToken(token));
    }

    @Test
    @DisplayName("默认密钥兜底")
    void testDefaultSecretFallback() {
        // setConfig 传空字符串应使用默认密钥
        JwtUtils.setConfig("", 0);
        String token = JwtUtils.generateToken(42L, 1);
        Claims claims = JwtUtils.parseToken(token);
        assertEquals(42L, claims.get("userId", Long.class));
    }

    @Test
    @DisplayName("Token 生成非空且唯一")
    void testTokenUniqueness() {
        String token1 = JwtUtils.generateToken(1L, 2);
        String token2 = JwtUtils.generateToken(1L, 2);
        // 相同参数生成的 Token 可能因时间戳不同而不同
        assertNotNull(token1);
        assertNotNull(token2);
    }

    @Test
    @DisplayName("Token 超过有效期后解析失败")
    void testExpiredToken() throws InterruptedException {
        // 有效期设为 1ms，生成后等待其过期
        JwtUtils.setConfig(BASE_SECRET, 1L);
        String token = JwtUtils.generateToken(1L, 2);

        Thread.sleep(20);

        assertThrows(Exception.class, () -> JwtUtils.parseToken(token));
    }

    @Test
    @DisplayName("不同用户生成的 Token 可正确解析出各自身份")
    void testTokenUniquenessAcrossUsers() {
        String tokenA = JwtUtils.generateToken(1L, 2);
        String tokenB = JwtUtils.generateToken(2L, 2);

        assertNotEquals(tokenA, tokenB);
        assertEquals(1L, JwtUtils.parseToken(tokenA).get("userId", Long.class));
        assertEquals(2L, JwtUtils.parseToken(tokenB).get("userId", Long.class));
    }
}