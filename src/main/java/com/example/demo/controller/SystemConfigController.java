package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.Result;
import com.example.demo.entity.SystemConfig;
import com.example.demo.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/config")
public class SystemConfigController {

    @Autowired
    private SystemConfigService configService;

    /**
     * 管理员分页查询配置列表
     */
    @GetMapping("/admin/list")
    @RequiresRoles({0})
    public Result<IPage<SystemConfig>> getAllConfigs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String configKey) {
        IPage<SystemConfig> page = configService.getAllConfigs(pageNum, pageSize, configKey);
        return Result.success(page);
    }

    /**
     * 管理员更新配置
     */
    @PutMapping("/admin/update")
    @RequiresRoles({0})
    public Result<String> updateConfig(@RequestBody SystemConfig config) {
        if (config.getId() == null) {
            return Result.fail("配置ID不能为空");
        }
        if (config.getConfigKey() == null || config.getConfigKey().trim().isEmpty()) {
            return Result.fail("配置键不能为空");
        }
        if (config.getConfigValue() == null || config.getConfigValue().trim().isEmpty()) {
            return Result.fail("配置值不能为空");
        }
        boolean success = configService.updateConfig(config);
        return success ? Result.success("更新成功") : Result.fail("更新失败");
    }

    /**
     * 管理员新增配置
     */
    @PostMapping("/admin/add")
    @RequiresRoles({0})
    public Result<String> addConfig(@RequestBody SystemConfig config) {
        if (config.getConfigKey() == null || config.getConfigKey().trim().isEmpty()) {
            return Result.fail("配置键不能为空");
        }
        if (config.getConfigValue() == null || config.getConfigValue().trim().isEmpty()) {
            return Result.fail("配置值不能为空");
        }
        boolean success = configService.addConfig(config);
        return success ? Result.success("添加成功") : Result.fail("添加失败");
    }

    /**
     * 管理员删除配置
     */
    @DeleteMapping("/admin/{configId}")
    @RequiresRoles({0})
    public Result<String> deleteConfig(@PathVariable Long configId) {
        boolean success = configService.deleteConfig(configId);
        return success ? Result.success("删除成功") : Result.fail("删除失败");
    }

    /**
     * 公开接口：获取平台名称（无需登录）
     */
    @GetMapping("/platform/name")
    public Result<String> getPlatformName() {
        String name = configService.getConfigValue("platform_name");
        return Result.success(name != null ? name : "安居房屋租赁平台");
    }

    /**
     * 公开接口：获取客服电话（无需登录）
     */
    @GetMapping("/platform/phone")
    public Result<String> getCustomerServicePhone() {
        String phone = configService.getConfigValue("customer_service_phone");
        return Result.success(phone != null ? phone : "400-888-8888");
    }
}