package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.entity.SystemConfig;

import java.math.BigDecimal;

public interface SystemConfigService {

    /**
     * 根据key获取配置值（供其他服务调用）
     */
    String getConfigValue(String key);

    /**
     * 根据key获取配置值并转为Integer
     */
    Integer getConfigInt(String key);

    /**
     * 根据key获取配置值并转为BigDecimal
     */
    BigDecimal getConfigDecimal(String key);

    /**
     * 管理员分页查询所有配置
     */
    IPage<SystemConfig> getAllConfigs(Integer pageNum, Integer pageSize, String configKey);

    /**
     * 管理员更新配置
     */
    boolean updateConfig(SystemConfig config);

    /**
     * 管理员新增配置
     */
    boolean addConfig(SystemConfig config);

    /**
     * 管理员删除配置
     */
    boolean deleteConfig(Long configId);
}