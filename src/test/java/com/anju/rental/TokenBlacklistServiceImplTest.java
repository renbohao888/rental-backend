package com.anju.rental;

import cn.hutool.crypto.digest.DigestUtil;
import com.anju.rental.common.JwtUtils;
import com.anju.rental.service.impl.TokenBlacklistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Token 黑名单服务单元测试（Mockito 模拟 Redis，不依赖真实 Redis 实例）。
 */
@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceImplTest {

    private static final String SECRET = "blacklist-test-secret-key-2025-abcdef";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenBlacklistServiceImpl service;

    @BeforeEach
    void setUp() {
        JwtUtils.setConfig(SECRET, 3600000L);
        service = new TokenBlacklistServiceImpl(stringRedisTemplate);
    }

    @Test
    @DisplayName("登出：以 MD5 摘要为 key 写入黑名单，TTL 不超过 Token 有效期")
    void testBlacklistWritesKeyWithTtl() {
        String token = JwtUtils.generateToken(1L, 2);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        service.blacklist(token);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(), eq("1"), ttlCaptor.capture());

        assertEquals("jwt:blacklist:" + DigestUtil.md5Hex(token), keyCaptor.getValue());
        // TTL 应介于 0 与配置有效期（1 小时）之间
        assertTrue(ttlCaptor.getValue().toMillis() > 0);
        assertTrue(ttlCaptor.getValue().toHours() <= 1);
    }

    @Test
    @DisplayName("登出：空 Token 不做任何操作")
    void testBlacklistWithBlankToken() {
        service.blacklist(null);
        service.blacklist("  ");

        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("登出：非法 / 已过期 Token 不写入黑名单")
    void testBlacklistWithInvalidToken() {
        service.blacklist("invalid-token");

        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("查询：命中黑名单返回 true")
    void testIsBlacklistedTrue() {
        String token = JwtUtils.generateToken(1L, 2);
        when(stringRedisTemplate.hasKey("jwt:blacklist:" + DigestUtil.md5Hex(token))).thenReturn(true);

        assertTrue(service.isBlacklisted(token));
    }

    @Test
    @DisplayName("查询：未命中黑名单返回 false")
    void testIsBlacklistedFalse() {
        String token = JwtUtils.generateToken(1L, 2);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        assertFalse(service.isBlacklisted(token));
    }

    @Test
    @DisplayName("fail-open：Redis 异常时按未拉黑处理，不阻断认证链路")
    void testIsBlacklistedFailOpen() {
        String token = JwtUtils.generateToken(1L, 2);
        when(stringRedisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("redis down"));

        assertFalse(service.isBlacklisted(token));
    }

    @Test
    @DisplayName("fail-open：Redis 写入异常不向上抛出")
    void testBlacklistSwallowsRedisError() {
        String token = JwtUtils.generateToken(1L, 2);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("redis down"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        assertDoesNotThrow(() -> service.blacklist(token));
    }
}
