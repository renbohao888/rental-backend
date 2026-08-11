package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.Banner;
import java.util.List;

public interface BannerService extends IService<Banner> {
    IPage<Banner> adminList(Integer pageNum, Integer pageSize);
    List<Banner> getEnabledBanners();
}