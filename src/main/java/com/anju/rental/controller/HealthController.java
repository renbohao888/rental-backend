package com.anju.rental.controller;

import com.anju.rental.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口（无需登录，已在 {@code WebConfig} 中放行）。
 *
 * <p>用于容器编排 / 负载均衡的就绪与存活探测，以及部署后快速自检。
 * 会分别探测 MySQL 与 Redis 的连通性，任一不可用时返回 {@code code=503}，
 * 便于调用方据此判断实例是否可对外提供服务。</p>
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class HealthController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    @Value("${spring.application.name:anju-house-rental-platform}")
    private String applicationName;

    public HealthController(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("database", checkDatabase());
        components.put("redis", checkRedis());

        boolean allUp = components.values().stream().allMatch("UP"::equals);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", allUp ? "UP" : "DOWN");
        data.put("application", applicationName);
        data.put("timestamp", LocalDateTime.now().format(TIME_FORMATTER));
        data.put("components", components);

        Result<Map<String, Object>> result = new Result<>();
        result.setCode(allUp ? 200 : 503);
        result.setMessage(allUp ? "服务正常" : "部分依赖不可用");
        result.setData(data);
        return result;
    }

    /**
     * 探测数据库连通性：校验连接是否可用。
     */
    private String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            log.warn("健康检查：数据库不可用 - {}", e.getMessage());
            return "DOWN";
        }
    }

    /**
     * 探测 Redis 连通性：发送 PING 命令。
     */
    private String checkRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            return "PONG".equalsIgnoreCase(connection.ping()) ? "UP" : "DOWN";
        } catch (Exception e) {
            log.warn("健康检查：Redis 不可用 - {}", e.getMessage());
            return "DOWN";
        }
    }
}
