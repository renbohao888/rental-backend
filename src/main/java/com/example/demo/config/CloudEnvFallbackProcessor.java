package com.example.demo.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 云端环境变量兜底处理器。
 *
 * <p>背景：Railway 等平台用 ${{OTHER_SERVICE_VAR}} 引用其他服务变量时，
 * 若变量名不匹配，会注入一个"空字符串"而非使变量缺失。
 * Spring 的占位符 ${REDIS_URL:default} 在"变量存在但为空"时不会使用默认值，
 * 导致空 host / 空 URL 等启动崩溃。</p>
 *
 * <p>本处理器在环境准备阶段（所有 Bean 创建之前）检查关键环境变量，
 * 若为空白则注入最高优先级的默认值，保证应用至少能正常启动。</p>
 */
public class CloudEnvFallbackProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> fixes = new HashMap<>();

        // Redis：为空则回退本地默认（无密码），保证启动不因空值崩溃
        if (isBlank(environment.getProperty("REDIS_URL"))) {
            fixes.put("REDIS_URL", "redis://localhost:6379");
        }
        if (isBlank(environment.getProperty("REDIS_PASSWORD"))) {
            fixes.put("REDIS_PASSWORD", "");
        }

        if (!fixes.isEmpty()) {
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("cloudEnvFallbackDefaults", fixes));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
