package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.Notice;
import com.example.demo.mapper.NoticeMapper;
import com.example.demo.service.NoticeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Override
    public boolean addNotice(Notice notice) {
        // 如果状态是已发布，设置发布时间
        if (notice.getStatus() != null && notice.getStatus() == 1) {
            notice.setPublishTime(LocalDateTime.now());
        }
        return this.save(notice);
    }

    @Override
    public boolean updateNotice(Notice notice) {
        // 如果状态改为已发布，更新发布时间
        if (notice.getStatus() != null && notice.getStatus() == 1) {
            notice.setPublishTime(LocalDateTime.now());
        }
        return this.updateById(notice);
    }

    @Override
    public boolean deleteNotice(Long noticeId) {
        return this.removeById(noticeId);
    }

    @Override
    public IPage<Notice> getAdminNoticeList(Integer pageNum, Integer pageSize, Integer status, String title) {
        Page<Notice> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Notice::getStatus, status);
        }
        if (StringUtils.hasText(title)) {
            wrapper.like(Notice::getTitle, title);
        }
        wrapper.orderByDesc(Notice::getIsTop)
                .orderByDesc(Notice::getPublishTime)
                .orderByDesc(Notice::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public IPage<Notice> getPublishedNoticeList(Integer pageNum, Integer pageSize, Integer type) {
        Page<Notice> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getStatus, 1) // 已发布
                .orderByDesc(Notice::getIsTop) // 置顶优先
                .orderByDesc(Notice::getPublishTime); // 发布时间倒序
        if (type != null) {
            wrapper.eq(Notice::getType, type);
        }
        return this.page(page, wrapper);
    }

    @Override
    public Notice getPublishedNoticeDetail(Long noticeId) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getId, noticeId)
                .eq(Notice::getStatus, 1); // 仅已发布的可见
        return this.getOne(wrapper);
    }
}