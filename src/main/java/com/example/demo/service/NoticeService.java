package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.entity.Notice;

public interface NoticeService {

    /**
     * 管理员：发布公告
     */
    boolean addNotice(Notice notice);

    /**
     * 管理员：更新公告
     */
    boolean updateNotice(Notice notice);

    /**
     * 管理员：删除公告（逻辑删除）
     */
    boolean deleteNotice(Long noticeId);

    /**
     * 管理员：分页查询所有公告（包含草稿）
     */
    IPage<Notice> getAdminNoticeList(Integer pageNum, Integer pageSize, Integer status, String title);

    /**
     * 用户端：分页查询已发布的公告（按置顶+发布时间降序）
     */
    IPage<Notice> getPublishedNoticeList(Integer pageNum, Integer pageSize, Integer type);

    /**
     * 用户端：根据ID查询公告详情（仅已发布的可见）
     */
    Notice getPublishedNoticeDetail(Long noticeId);
}