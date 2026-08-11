package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.Result;
import com.example.demo.entity.Notice;
import com.example.demo.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    // ================== 管理员端接口 ==================

    /**
     * 管理员：发布/新增公告
     */
    @PostMapping("/add")
    @RequiresRoles({0})
    public Result<String> addNotice(@RequestBody Notice notice) {
        if (notice.getTitle() == null || notice.getTitle().trim().isEmpty()) {
            return Result.fail("公告标题不能为空");
        }
        if (notice.getContent() == null || notice.getContent().trim().isEmpty()) {
            return Result.fail("公告内容不能为空");
        }
        boolean success = noticeService.addNotice(notice);
        return success ? Result.success("公告发布成功") : Result.fail("公告发布失败");
    }

    /**
     * 管理员：更新公告
     */
    @PutMapping("/update")
    @RequiresRoles({0})
    public Result<String> updateNotice(@RequestBody Notice notice) {
        if (notice.getId() == null) {
            return Result.fail("公告ID不能为空");
        }
        boolean success = noticeService.updateNotice(notice);
        return success ? Result.success("公告更新成功") : Result.fail("公告更新失败");
    }

    /**
     * 管理员：删除公告（逻辑删除）
     */
    @DeleteMapping("/admin/delete/{noticeId}")
    @RequiresRoles({0})
    public Result<String> deleteNotice(@PathVariable Long noticeId) {
        boolean success = noticeService.deleteNotice(noticeId);
        return success ? Result.success("公告删除成功") : Result.fail("公告删除失败");
    }

    /**
     * 管理员：分页查询所有公告（含草稿）
     */
    @GetMapping("/admin/list")
    @RequiresRoles({0})
    public Result<IPage<Notice>> getAdminNoticeList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String title) {
        IPage<Notice> page = noticeService.getAdminNoticeList(pageNum, pageSize, status, title);
        return Result.success(page);
    }

    // ================== 用户端接口（公开，无需Token） ==================

    /**
     * 用户端：分页查询已发布的公告列表
     * 游客、租客、房东均可查看
     */
    @GetMapping("/list")
    public Result<IPage<Notice>> getPublishedNoticeList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer type) {
        IPage<Notice> page = noticeService.getPublishedNoticeList(pageNum, pageSize, type);
        return Result.success(page);
    }

    /**
     * 用户端：查看公告详情（仅已发布的可见）
     * 使用独立子路径 /detail/**，避免与 /add、/update 等管理端接口在
     * WebConfig 的 excludePathPatterns 中冲突导致绕过鉴权
     */
    @GetMapping("/detail/{noticeId}")
    public Result<Notice> getPublishedNoticeDetail(@PathVariable Long noticeId) {
        Notice notice = noticeService.getPublishedNoticeDetail(noticeId);
        if (notice == null) {
            return Result.fail("公告不存在或未发布");
        }
        return Result.success(notice);
    }
}