package com.anju.rental.common;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Redis 分布式锁工具（SETNX + 过期时间 + Lua 校验释放）
 *
 * <p>设计要点：</p>
 * <ol>
 *   <li>tryLock 通过 set(key, value, NX, EX) 原子加锁，同一时刻只有拿到锁的线程能进入临界区；</li>
 *   <li>lockValue 每次调用随机生成（UUID），作为“锁持有者标识”，避免并发请求间 value 相同导致误判；</li>
 *   <li>unlock 用 Lua 脚本先比对持有者再删除，防止“业务执行超过锁过期时间后，旧线程把新线程的锁误删”。</li>
 * </ol>
 */
@Component
public class RedisLockUtil {

    /** 释放锁的 Lua 脚本：仅当 KEY 的 value 与当前持有者一致时才删除（原子操作） */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisLockUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** 生成随机锁持有者标识，每个线程每次加锁都应使用新值 */
    public String generateLockValue() {
        return UUID.randomUUID().toString();
    }

    /**
     * 尝试加锁（非阻塞）：key 不存在则写入并返回 true；已被他人持有则立即返回 false。
     *
     * @param lockKey       锁的 key
     * @param lockValue     锁持有者标识（由 generateLockValue 生成）
     * @param expireSeconds 锁自动过期时间，防止持有者异常宕机导致死锁
     */
    public boolean tryLock(String lockKey, String lockValue, long expireSeconds) {
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(expireSeconds));
        return Boolean.TRUE.equals(success);
    }

    /** 释放锁：持有者标识匹配才删除，避免误删他人刚获取的锁 */
    public void unlock(String lockKey, String lockValue) {
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), lockValue);
    }
}
