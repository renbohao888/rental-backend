package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.Result;
import com.example.demo.entity.Banner;
import com.example.demo.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    // ================== 公开接口（无需登录） ==================

    /**
     * 用户端：获取首页启用的轮播图列表
     */
    @GetMapping("/list")
    public Result<List<Banner>> getEnabledBanners() {
        List<Banner> list = bannerService.getEnabledBanners();
        return Result.success(list);
    }

    // ================== 管理员端接口 ==================

    /**
     * 管理员：分页查询轮播图列表
     */
    @GetMapping("/admin/list")
    @RequiresRoles({0})
    public Result<IPage<Banner>> adminList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<Banner> page = bannerService.adminList(pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 管理员：新增轮播图
     */
    @PostMapping("/admin/add")
    @RequiresRoles({0})
    public Result<String> addBanner(@RequestBody Banner banner) {
        if (banner.getImageUrl() == null || banner.getImageUrl().trim().isEmpty()) {
            return Result.fail("图片地址不能为空");
        }
        // 默认启用
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        boolean success = bannerService.save(banner);
        return success ? Result.success("添加成功") : Result.fail("添加失败");
    }

    /**
     * 管理员：更新轮播图
     */
    @PutMapping("/admin/update")
    @RequiresRoles({0})
    public Result<String> updateBanner(@RequestBody Banner banner) {
        if (banner.getId() == null) {
            return Result.fail("ID不能为空");
        }
        boolean success = bannerService.updateById(banner);
        return success ? Result.success("更新成功") : Result.fail("更新失败");
    }

    /**
     * 管理员：删除轮播图
     */
    @DeleteMapping("/admin/{bannerId}")
    @RequiresRoles({0})
    public Result<String> deleteBanner(@PathVariable Long bannerId) {
        boolean success = bannerService.removeById(bannerId);
        return success ? Result.success("删除成功") : Result.fail("删除失败");
    }
}