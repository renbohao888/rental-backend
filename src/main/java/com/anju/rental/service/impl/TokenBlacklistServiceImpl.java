package com.anju.rental.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.anju.rental.common.JwtUtils;
import com.anju.rental.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Token 黑名单服务实现（基于 Redis）。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li><b>Key 不直接使用原始 Token</b>：用 MD5 摘要作为 key 的一部分，避免长字符串与特殊字符带来的问题；</li>
 *   <li><b>TTL 与 Token 剩余有效期一致</b>：无需额外的清理任务，Token 自然过期后黑名单记录也随之消失；</li>
 *   <li><b>fail-open</b>：Redis 不可用时（读取异常）视为「未被拉黑」并打日志，保证认证链路不因缓存故障整体不可用。</li>
 * </ul>
 */
@Service
@Slf4j
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    /** Redis key 前缀 */
    private static final String KEY_PREFIX = "jwt:blacklist:";

    /** 兜底 TTL：解析不出过期时间时（不应发生），最多保留 1 天 */
    private static final Duration FALLBACK_TTL = Duration.ofDays(1);

    private final StringRedisTemplate stringRedisTemplate;

    public TokenBlacklistServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void blacklist(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        // Token 已经过期就没有拉黑的必要
        long ttlMillis = remainingTtlMillis(token);
        if (ttlMillis <= 0) {
            log.debug("Token 已过期，跳过加入黑名单");
            return;
        }

        try {
            stringRedisTemplate.opsForValue()
                    .set(redisKey(token), "1", Duration.ofMillis(ttlMillis));
            log.debug("Token 已加入黑名单，剩余有效期 {} ms", ttlMillis);
        } catch (Exception e) {
            // 登出属于「尽力而为」：即使 Redis 异常也不应让用户卡在登录态
            log.warn("写入 Token 黑名单失败（Redis 异常）：{}", e.getMessage());
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey(token)));
        } catch (Exception e) {
            // fail-open：Redis 异常时放行，避免认证整体不可用
            log.warn("读取 Token 黑名单失败（Redis 异常），本次按未拉黑处理：{}", e.getMessage());
            return false;
        }
    }

    /* ---------------- 私有工具方法 ---------------- */

    private String redisKey(String token) {
        return KEY_PREFIX + DigestUtil.md5Hex(token);
    }

    /**
     * 计算 Token 的剩余有效毫秒数；无法解析或已过期返回 0 或负数。
     */
    private long remainingTtlMillis(String token) {
        try {
            Claims claims = JwtUtils.parseToken(token);
            Date expiration = claims.getExpiration();
            if (expiration == null) {
                return FALLBACK_TTL.toMillis();
            }
            return expiration.getTime() - Instant.now().toEpochMilli();
        } catch (Exception e) {
            // 解析失败（非法 / 过期 / 签名不符），视为无需拉黑
            log.debug("解析 Token 过期时间失败，跳过黑名单：{}", e.getMessage());
            return 0L;
        }
    }
}
