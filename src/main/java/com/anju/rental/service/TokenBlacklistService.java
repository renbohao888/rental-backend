package com.anju.rental.service;

/**
 * Token 黑名单服务：用于用户登出后立即失效已签发的 JWT。
 *
 * <p>JWT 是无状态令牌，签发后在有效期内始终可用。为支持「主动登出 / 强制下线」，
 * 登出时把 Token 写入黑名单（Redis），有效期取该 Token 的剩余存活时间，
 * 拦截器每次校验时命中黑名单即视为登录失效。</p>
 */
public interface TokenBlacklistService {

    /**
     * 将 Token 加入黑名单，到期时间与该 Token 自身的过期时间对齐。
     * <p>Token 已过期或无法解析时不做处理。</p>
     *
     * @param token 原始 JWT 字符串
     */
    void blacklist(String token);

    /**
     * 判断 Token 是否已被拉黑。
     *
     * @param token 原始 JWT 字符串
     * @return true 表示该 Token 已登出 / 失效
     */
    boolean isBlacklisted(String token);
}
