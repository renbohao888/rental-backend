package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.SystemConfig;
import com.example.demo.mapper.SystemConfigMapper;
import com.example.demo.service.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    @Override
    public String getConfigValue(String key) {
        if (configCache.containsKey(key)) {
            return configCache.get(key);
        }
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = this.getOne(wrapper);
        if (config != null) {
            configCache.put(key, config.getConfigValue());
            return config.getConfigValue();
        }
        return null;
    }

    @Override
    public Integer getConfigInt(String key) {
        String value = getConfigValue(key);
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public BigDecimal getConfigDecimal(String key) {
        String value = getConfigValue(key);
        if (value == null) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public IPage<SystemConfig> getAllConfigs(Integer pageNum, Integer pageSize, String configKey) {
        Page<SystemConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(configKey)) {
            wrapper.like(SystemConfig::getConfigKey, configKey);
        }
        wrapper.orderByAsc(SystemConfig::getConfigKey);
        return this.page(page, wrapper);
    }

    @Override
    public boolean updateConfig(SystemConfig config) {
        SystemConfig oldConfig = this.getById(config.getId());
        if (oldConfig == null) {
            throw new RuntimeException("配置不存在");
        }
        configCache.remove(oldConfig.getConfigKey());
        if (!oldConfig.getConfigKey().equals(config.getConfigKey())) {
            configCache.remove(config.getConfigKey());
        }
        return this.updateById(config);
    }

    @Override
    public boolean addConfig(SystemConfig config) {
        return this.save(config);
    }

    @Override
    public boolean deleteConfig(Long configId) {
        SystemConfig config = this.getById(configId);
        if (config != null) {
            configCache.remove(config.getConfigKey());
        }
        return this.removeById(configId);
    }
}